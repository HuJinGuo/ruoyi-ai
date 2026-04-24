package org.ruoyi.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class VectorStorePropertiesTest {

    @Test
    void should_support_pgvector_properties() {
        VectorStoreProperties properties = new VectorStoreProperties();
        VectorStoreProperties.Pgvector pgvector = new VectorStoreProperties.Pgvector();
        pgvector.setDatasourceUrl("jdbc:postgresql://127.0.0.1:5432/ruoyi_ai");
        pgvector.setUsername("openclaw");
        pgvector.setPassword("openclaw");
        pgvector.setSchema("public");
        pgvector.setTablePrefix("local_knowledge");
        pgvector.setUseIndex(true);
        pgvector.setIndexListSize(100);
        pgvector.setCreateTable(true);
        pgvector.setDropTableFirst(false);

        properties.setPgvector(pgvector);

        assertEquals("jdbc:postgresql://127.0.0.1:5432/ruoyi_ai", properties.getPgvector().getDatasourceUrl());
        assertEquals("public", properties.getPgvector().getSchema());
        assertEquals("local_knowledge", properties.getPgvector().getTablePrefix());
        assertTrue(properties.getPgvector().isUseIndex());
        assertEquals(100, properties.getPgvector().getIndexListSize());
    }
}
