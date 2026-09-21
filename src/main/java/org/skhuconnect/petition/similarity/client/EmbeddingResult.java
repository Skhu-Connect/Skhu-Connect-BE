package org.skhuconnect.petition.similarity.client;

public record EmbeddingResult(
        String modelName,
        int dimensions,
        float[] vector
) {
}
