package org.skhuconnect.petition.similarity.client;

import org.skhuconnect.petition.similarity.config.OpenAiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private static final String EMBEDDINGS_PATH = "/v1/embeddings";

    private final RestClient restClient;
    private final OpenAiProperties properties;

    @Autowired
    public OpenAiEmbeddingClient(OpenAiProperties properties) {
        this(RestClient.builder(), properties);
    }

    OpenAiEmbeddingClient(RestClient.Builder builder, OpenAiProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl("https://api.openai.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION,
                        "Bearer " + nullToBlank(properties.getApiKey()))
                .build();
    }

    @Override
    public EmbeddingResult embed(String input) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new EmbeddingClientException("OpenAI API key is not configured");
        }
        try {
            EmbeddingResponse response = restClient.post()
                    .uri(EMBEDDINGS_PATH)
                    .body(Map.of(
                            "model", properties.getEmbeddingModel(),
                            "input", input,
                            "dimensions", properties.getEmbeddingDimensions()
                    ))
                    .retrieve()
                    .body(EmbeddingResponse.class);
            if (response == null || response.data() == null || response.data().isEmpty()
                    || response.data().get(0).embedding() == null) {
                throw new EmbeddingClientException("OpenAI embedding response is empty");
            }
            List<Double> values = response.data().get(0).embedding();
            if (values.size() != properties.getEmbeddingDimensions()) {
                throw new EmbeddingClientException("OpenAI embedding dimensions mismatch");
            }
            float[] vector = new float[values.size()];
            for (int index = 0; index < values.size(); index++) {
                vector[index] = values.get(index).floatValue();
            }
            return new EmbeddingResult(
                    properties.getEmbeddingModel(),
                    properties.getEmbeddingDimensions(),
                    vector
            );
        } catch (RestClientException exception) {
            throw new EmbeddingClientException("OpenAI embedding request failed", exception);
        }
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private record EmbeddingResponse(List<EmbeddingData> data) {
    }

    private record EmbeddingData(List<Double> embedding) {
    }
}
