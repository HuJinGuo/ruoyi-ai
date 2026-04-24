package org.ruoyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 向量库配置属性
 *
 * @author ageer
 */
@Data
@Component
@ConfigurationProperties(prefix = "vector-store")
public class VectorStoreProperties {

    /**
     * 向量库类型
     */
    private String type;

    /**
     * Weaviate配置
     */
    private Weaviate weaviate = new Weaviate();

    /**
     * Milvus配置
     */
    private Milvus milvus = new Milvus();

    /**
     * Pgvector配置
     */
    private Pgvector pgvector = new Pgvector();

    @Data
    public static class Weaviate {
        /**
         * 协议
         */
        private String protocol;

        /**
         * 主机地址
         */
        private String host;

        /**
         * 类名
         */
        private String classname;
    }

    @Data
    public static class Milvus {
        /**
         * 连接URL
         */
        private String url;

        /**
         * 集合名称
         */
        private String collectionname;
    }

    /**
     * Qdrant配置
     */
    private Qdrant qdrant = new Qdrant();

    @Data
    public static class Qdrant {
        /**
         * 主机地址
         */
        private String host = "localhost";

        /**
         * gRPC端口
         */
        private int port = 6334;

        /**
         * 集合名称
         */
        private String collectionname = "LocalKnowledge";

        /**
         * API密钥（可选）
         */
        private String apiKey;

        /**
         * 是否启用TLS
         */
        private boolean useTls = false;
    }

    @Data
    public static class Pgvector {
        /**
         * PostgreSQL 数据源地址
         */
        private String datasourceUrl;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        /**
         * schema
         */
        private String schema = "public";

        /**
         * 表名前缀
         */
        private String tablePrefix = "local_knowledge";

        /**
         * 是否创建 ivfflat 索引
         */
        private boolean useIndex = true;

        /**
         * ivfflat lists 参数
         */
        private int indexListSize = 100;

        /**
         * 是否自动建表
         */
        private boolean createTable = true;

        /**
         * 是否先删表再重建
         */
        private boolean dropTableFirst = false;
    }
}
