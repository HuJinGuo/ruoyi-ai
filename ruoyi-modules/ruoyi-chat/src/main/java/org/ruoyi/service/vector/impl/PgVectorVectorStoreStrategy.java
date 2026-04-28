package org.ruoyi.service.vector.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.bo.vector.StoreEmbeddingBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.factory.EmbeddingModelFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * PgVector 向量库策略实现
 */
@Slf4j
@Component
public class PgVectorVectorStoreStrategy extends AbstractVectorStoreStrategy {

    private static final String VECTOR_STORE_TYPE = "pgvector";

    private volatile HikariDataSource dataSource;

    public PgVectorVectorStoreStrategy(VectorStoreProperties vectorStoreProperties,
                                       IChatModelService chatModelService,
                                       EmbeddingModelFactory embeddingModelFactory) {
        super(vectorStoreProperties, embeddingModelFactory, chatModelService);
    }

    @Override
    public String getVectorStoreType() {
        return VECTOR_STORE_TYPE;
    }

    @Override
    public void createSchema(String kid, String modelName) {
        ensureSchema(kid, modelName, true);
    }

    private void ensureSchema(String kid, String modelName, boolean allowDropTableFirst) {
        VectorStoreProperties.Pgvector cfg = getPgvectorConfig();
        String qualifiedTable = qualifiedTableName(kid);
        int dimension = getModelDimension(modelName);
        try (Connection connection = getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + quoteIdentifier(cfg.getSchema()));
            if (allowDropTableFirst && cfg.isDropTableFirst()) {
                statement.execute("DROP TABLE IF EXISTS " + qualifiedTable);
            }
            if (cfg.isCreateTable()) {
                statement.execute("CREATE TABLE IF NOT EXISTS " + qualifiedTable + " ("
                    + "id BIGSERIAL PRIMARY KEY,"
                    + "content TEXT NOT NULL,"
                    + "embedding vector(" + dimension + ") NOT NULL,"
                    + "fid VARCHAR(255),"
                    + "kid VARCHAR(255),"
                    + "doc_id VARCHAR(255)"
                    + ")");
            }
            if (cfg.isUseIndex()) {
                statement.execute("CREATE INDEX IF NOT EXISTS " + quoteIdentifier(indexName(kid))
                    + " ON " + qualifiedTable
                    + " USING ivfflat (embedding vector_cosine_ops) WITH (lists = " + cfg.getIndexListSize() + ")");
            }
            log.info("PgVector表初始化完成: {}, dimension: {}", qualifiedTable, dimension);
        } catch (SQLException e) {
            log.error("PgVector表初始化失败: {}", qualifiedTable, e);
            throw new ServiceException("PgVector表初始化失败: " + qualifiedTable);
        }
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) throws ServiceException {
        ensureSchema(storeEmbeddingBo.getKid(), storeEmbeddingBo.getEmbeddingModelName(), false);
        EmbeddingModel embeddingModel = getEmbeddingModel(storeEmbeddingBo.getEmbeddingModelName());
        List<String> chunkList = storeEmbeddingBo.getChunkList();
        List<String> fidList = storeEmbeddingBo.getFids();
        String kid = storeEmbeddingBo.getKid();
        String docId = storeEmbeddingBo.getDocId();
        String qualifiedTable = qualifiedTableName(kid);

        String sql = "INSERT INTO " + qualifiedTable + " (content, embedding, fid, kid, doc_id) VALUES (?, CAST(? AS vector), ?, ?, ?)";

        log.info("PgVector向量存储条数记录: {}", chunkList.size());
        long startTime = System.currentTimeMillis();

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            IntStream.range(0, chunkList.size()).forEach(i -> {
                String text = chunkList.get(i);
                String fid = fidList.get(i);
                Embedding embedding = embeddingModel.embed(text).content();
                try {
                    ps.setString(1, text);
                    ps.setString(2, toPgVectorLiteral(embedding.vector()));
                    ps.setString(3, fid);
                    ps.setString(4, kid);
                    ps.setString(5, docId);
                    ps.addBatch();
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
            ps.executeBatch();
        } catch (IllegalStateException e) {
            log.error("PgVector向量批量写入失败: {}", qualifiedTable, e);
            throw new ServiceException("PgVector向量写入失败");
        } catch (SQLException e) {
            log.error("PgVector向量批量写入失败: {}", qualifiedTable, e);
            throw new ServiceException("PgVector向量写入失败");
        }

        long endTime = System.currentTimeMillis();
        log.info("PgVector向量存储完成消耗时间：{}秒", (endTime - startTime) / 1000);
    }

    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        ensureSchema(queryVectorBo.getKid(), queryVectorBo.getEmbeddingModelName(), false);
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName());
        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        String qualifiedTable = qualifiedTableName(queryVectorBo.getKid());
        String sql = "SELECT content FROM " + qualifiedTable + " ORDER BY embedding <=> CAST(? AS vector) LIMIT ?";

        List<String> resultList = new ArrayList<>();
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, toPgVectorLiteral(queryEmbedding.vector()));
            ps.setInt(2, queryVectorBo.getMaxResults());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultList.add(rs.getString("content"));
                }
            }
            return resultList;
        } catch (SQLException e) {
            log.error("PgVector查询失败: {}", qualifiedTable, e);
            throw new ServiceException("PgVector向量查询失败");
        }
    }

    @Override
    public List<KnowledgeRetrievalVo> search(QueryVectorBo queryVectorBo) {
        ensureSchema(queryVectorBo.getKid(), queryVectorBo.getEmbeddingModelName(), false);
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName());
        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        String qualifiedTable = qualifiedTableName(queryVectorBo.getKid());
        String sql = "SELECT content, doc_id, (1 - (embedding <=> CAST(? AS vector))) AS score "
            + "FROM " + qualifiedTable + " ORDER BY embedding <=> CAST(? AS vector) LIMIT ?";

        List<KnowledgeRetrievalVo> resultList = new ArrayList<>();
        String queryVector = toPgVectorLiteral(queryEmbedding.vector());
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, queryVector);
            ps.setString(2, queryVector);
            ps.setInt(3, queryVectorBo.getMaxResults());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultList.add(KnowledgeRetrievalVo.builder()
                        .content(rs.getString("content"))
                        .docId(rs.getString("doc_id"))
                        .score(rs.getDouble("score"))
                        .sourceName("未知来源")
                        .build());
                }
            }
            return resultList;
        } catch (SQLException e) {
            log.error("PgVector检索失败: {}", qualifiedTable, e);
            throw new ServiceException("PgVector向量检索失败");
        }
    }

    @Override
    public void removeById(String id, String modelName) throws ServiceException {
        String qualifiedTable = qualifiedTableName(id);
        try (Connection connection = getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + qualifiedTable);
            log.info("PgVector成功删除表: {}", qualifiedTable);
        } catch (SQLException e) {
            log.error("PgVector删除表失败: {}", qualifiedTable, e);
            throw new ServiceException("失败删除向量数据!");
        }
    }

    @Override
    public void removeByDocId(String docId, String kid) throws ServiceException {
        deleteByField(kid, "doc_id", docId, "docId");
    }

    @Override
    public void removeByFid(String fid, String kid) throws ServiceException {
        deleteByField(kid, "fid", fid, "fid");
    }

    private void deleteByField(String kid, String columnName, String value, String logFieldName) {
        String qualifiedTable = qualifiedTableName(kid);
        String sql = "DELETE FROM " + qualifiedTable + " WHERE " + columnName + " = ?";
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.executeUpdate();
            log.info("PgVector成功删除 {}={} 的所有向量数据", logFieldName, value);
        } catch (SQLException e) {
            log.error("PgVector删除失败: table={}, {}={}", qualifiedTable, logFieldName, value, e);
            throw new ServiceException("PgVector删除向量数据失败");
        }
    }

    private DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    VectorStoreProperties.Pgvector cfg = getPgvectorConfig();
                    HikariConfig hikariConfig = new HikariConfig();
                    hikariConfig.setJdbcUrl(cfg.getDatasourceUrl());
                    hikariConfig.setUsername(cfg.getUsername());
                    hikariConfig.setPassword(cfg.getPassword());
                    hikariConfig.setDriverClassName("org.postgresql.Driver");
                    hikariConfig.setMaximumPoolSize(3);
                    hikariConfig.setMinimumIdle(1);
                    hikariConfig.setPoolName("ruoyi-chat-pgvector");
                    dataSource = new HikariDataSource(hikariConfig);
                }
            }
        }
        return dataSource;
    }

    private VectorStoreProperties.Pgvector getPgvectorConfig() {
        VectorStoreProperties.Pgvector cfg = vectorStoreProperties.getPgvector();
        if (cfg == null || cfg.getDatasourceUrl() == null || cfg.getDatasourceUrl().isBlank()) {
            throw new ServiceException("未配置 PgVector 数据源");
        }
        return cfg;
    }

    private int getModelDimension(String modelName) {
        ChatModelVo modelConfig = chatModelService.selectModelByName(modelName);
        return modelConfig.getModelDimension();
    }

    private String qualifiedTableName(String kid) {
        VectorStoreProperties.Pgvector cfg = getPgvectorConfig();
        return quoteIdentifier(cfg.getSchema()) + "." + quoteIdentifier(rawTableName(kid));
    }

    private String rawTableName(String kid) {
        return (getPgvectorConfig().getTablePrefix() + "_" + kid)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9_]", "_");
    }

    private String indexName(String kid) {
        return rawTableName(kid) + "_embedding_idx";
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private String toPgVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
