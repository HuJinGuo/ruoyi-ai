package org.ruoyi.factory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.service.vector.VectorStoreService;
import org.ruoyi.service.vector.impl.MilvusVectorStoreStrategy;
import org.ruoyi.service.vector.impl.PgVectorVectorStoreStrategy;
import org.ruoyi.service.vector.impl.QdrantVectorStoreStrategy;
import org.ruoyi.service.vector.impl.WeaviateVectorStoreStrategy;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

@Tag("dev")
class VectorStoreStrategyFactoryTest {

    @Test
    void should_return_pgvector_strategy_when_type_is_pgvector() {
        VectorStoreProperties properties = new VectorStoreProperties();
        properties.setType("pgvector");

        WeaviateVectorStoreStrategy weaviate = mock(WeaviateVectorStoreStrategy.class);
        MilvusVectorStoreStrategy milvus = mock(MilvusVectorStoreStrategy.class);
        QdrantVectorStoreStrategy qdrant = mock(QdrantVectorStoreStrategy.class);
        PgVectorVectorStoreStrategy pgvector = mock(PgVectorVectorStoreStrategy.class);

        VectorStoreStrategyFactory factory = new VectorStoreStrategyFactory(properties, weaviate, milvus, qdrant, pgvector);
        factory.init();

        VectorStoreService actual = factory.getStrategy();

        assertSame(pgvector, actual);
    }
}
