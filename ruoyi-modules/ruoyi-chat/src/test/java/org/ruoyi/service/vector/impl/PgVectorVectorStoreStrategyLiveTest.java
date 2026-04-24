package org.ruoyi.service.vector.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.bo.vector.StoreEmbeddingBo;
import org.ruoyi.factory.EmbeddingModelFactory;
import org.ruoyi.service.embed.BaseEmbedModelService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class PgVectorVectorStoreStrategyLiveTest {

    @Test
    void should_store_query_and_delete_embeddings_in_pgvector() {
        String kid = "itest_pgvector_live";
        String modelName = "test-embedding-model";

        VectorStoreProperties properties = new VectorStoreProperties();
        properties.setType("pgvector");

        VectorStoreProperties.Pgvector pgvector = new VectorStoreProperties.Pgvector();
        pgvector.setDatasourceUrl("jdbc:postgresql://127.0.0.1:5432/ruoyi_ai?currentSchema=public&ApplicationName=ruoyi-ai-test");
        pgvector.setUsername("openclaw");
        pgvector.setPassword("openclaw");
        pgvector.setSchema("public");
        pgvector.setTablePrefix("local_knowledge");
        pgvector.setUseIndex(false);
        pgvector.setCreateTable(true);
        pgvector.setDropTableFirst(true);
        properties.setPgvector(pgvector);

        IChatModelService chatModelService = mock(IChatModelService.class);
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setModelName(modelName);
        modelVo.setModelDimension(3);
        when(chatModelService.selectModelByName(modelName)).thenReturn(modelVo);

        EmbeddingModelFactory embeddingModelFactory = mock(EmbeddingModelFactory.class);
        BaseEmbedModelService embeddingModel = mock(BaseEmbedModelService.class);
        when(embeddingModelFactory.createModel(modelName)).thenReturn(embeddingModel);

        Map<String, float[]> vectors = Map.of(
            "hello", new float[]{1.0f, 0.0f, 0.0f},
            "world", new float[]{0.0f, 1.0f, 0.0f},
            "hello query", new float[]{0.9f, 0.1f, 0.0f}
        );
        when(embeddingModel.embed(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0, String.class);
            float[] vector = vectors.get(text);
            return Response.from(Embedding.from(vector));
        });

        PgVectorVectorStoreStrategy strategy = new PgVectorVectorStoreStrategy(properties, chatModelService, embeddingModelFactory);
        try {
            StoreEmbeddingBo storeBo = new StoreEmbeddingBo();
            storeBo.setKid(kid);
            storeBo.setDocId("doc-1");
            storeBo.setEmbeddingModelName(modelName);
            storeBo.setChunkList(List.of("hello", "world"));
            storeBo.setFids(List.of("fid-1", "fid-2"));
            strategy.storeEmbeddings(storeBo);

            QueryVectorBo queryBo = new QueryVectorBo();
            queryBo.setKid(kid);
            queryBo.setQuery("hello query");
            queryBo.setMaxResults(1);
            queryBo.setEmbeddingModelName(modelName);
            List<String> result = strategy.getQueryVector(queryBo);
            assertEquals(1, result.size());
            assertEquals("hello", result.get(0));

            strategy.removeByDocId("doc-1", kid);
            List<String> emptyResult = strategy.getQueryVector(queryBo);
            assertTrue(emptyResult.isEmpty());
        } finally {
            strategy.removeById(kid, modelName);
        }
    }
}
