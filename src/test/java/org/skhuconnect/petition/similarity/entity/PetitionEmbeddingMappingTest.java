package org.skhuconnect.petition.similarity.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;
import org.skhuconnect.petition.similarity.service.EmbeddingVectorCodec;

import static org.assertj.core.api.Assertions.assertThat;

class PetitionEmbeddingMappingTest {

    @Test
    void embeddingColumnsUseBlobDefinitionForMysqlValidation() throws Exception {
        Column embedding = PetitionEmbedding.class
                .getDeclaredField("embedding")
                .getAnnotation(Column.class);
        Column queryEmbedding = PetitionSimilaritySearchLog.class
                .getDeclaredField("queryEmbedding")
                .getAnnotation(Column.class);

        assertThat(embedding.columnDefinition()).isEqualTo("BLOB");
        assertThat(queryEmbedding.columnDefinition()).isEqualTo("BLOB");
    }

    @Test
    void configuredEmbeddingVectorRequiresMoreThanTinyBlobCapacity() {
        byte[] encoded = EmbeddingVectorCodec.encode(new float[256]);

        assertThat(encoded).hasSize(1024);
    }
}
