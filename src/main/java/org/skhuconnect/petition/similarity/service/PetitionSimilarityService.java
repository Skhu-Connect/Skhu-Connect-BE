package org.skhuconnect.petition.similarity.service;

import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.similarity.client.EmbeddingClient;
import org.skhuconnect.petition.similarity.client.EmbeddingClientException;
import org.skhuconnect.petition.similarity.client.EmbeddingResult;
import org.skhuconnect.petition.similarity.config.OpenAiProperties;
import org.skhuconnect.petition.similarity.config.PetitionSimilarityProperties;
import org.skhuconnect.petition.similarity.dto.PetitionSimilarityRequest;
import org.skhuconnect.petition.similarity.dto.PetitionSimilarityResponse;
import org.skhuconnect.petition.similarity.dto.PetitionSimilarityUsageResponse;
import org.skhuconnect.petition.similarity.dto.SimilarPetitionResponse;
import org.skhuconnect.petition.similarity.entity.PetitionEmbedding;
import org.skhuconnect.petition.similarity.entity.PetitionEmbeddingStatus;
import org.skhuconnect.petition.similarity.entity.PetitionSimilaritySearchLog;
import org.skhuconnect.petition.similarity.exception.PetitionSimilarityException;
import org.skhuconnect.petition.similarity.repository.PetitionEmbeddingRepository;
import org.skhuconnect.petition.similarity.repository.PetitionSimilaritySearchLogRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class PetitionSimilarityService {

    private final UserRepository users;
    private final PetitionEmbeddingRepository embeddings;
    private final PetitionSimilaritySearchLogRepository searchLogs;
    private final EmbeddingClient embeddingClient;
    private final OpenAiProperties openAiProperties;
    private final PetitionSimilarityProperties properties;
    private final PetitionSimilarityUsage usage;
    private final Clock clock;

    public PetitionSimilarityService(
            UserRepository users,
            PetitionEmbeddingRepository embeddings,
            PetitionSimilaritySearchLogRepository searchLogs,
            EmbeddingClient embeddingClient,
            OpenAiProperties openAiProperties,
            PetitionSimilarityProperties properties,
            PetitionSimilarityUsage usage,
            Clock clock
    ) {
        this.users = users;
        this.embeddings = embeddings;
        this.searchLogs = searchLogs;
        this.embeddingClient = embeddingClient;
        this.openAiProperties = openAiProperties;
        this.properties = properties;
        this.usage = usage;
        this.clock = clock;
    }

    public PetitionSimilarityResponse findSimilar(Long userId, PetitionSimilarityRequest request) {
        User user = users.findByIdAndDeletedFalse(userId)
                .filter(value -> !value.isLoginBanned())
                .orElseThrow(() -> new PetitionSimilarityException(
                        PetitionSimilarityException.Reason.USER_NOT_FOUND));
        String text = PetitionEmbeddingText.from(request.title(), request.content());
        String queryHash = PetitionContentHasher.sha256(text);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime windowStart = now.minus(properties.getSearchWindow());
        String modelName = openAiProperties.getEmbeddingModel();
        int dimensions = openAiProperties.getEmbeddingDimensions();

        PetitionSimilaritySearchLog cached = searchLogs
                .findTopByUserIdAndQueryHashAndModelNameAndDimensionsAndCreatedAtAfterOrderByCreatedAtDesc(
                        userId, queryHash, modelName, dimensions, windowStart)
                .orElse(null);
        boolean cacheHit = cached != null;

        float[] queryVector;
        if (cacheHit) {
            queryVector = EmbeddingVectorCodec.decode(cached.getQueryEmbedding());
        } else {
            validateUsage(userId, now, windowStart);
            queryVector = createQueryEmbedding(text);
        }

        List<SimilarPetitionResponse> results = search(queryVector, modelName, dimensions, now);
        if (!cacheHit) {
            searchLogs.save(PetitionSimilaritySearchLog.counted(
                    user, queryHash, modelName, dimensions,
                    EmbeddingVectorCodec.encode(queryVector)));
        }
        PetitionSimilarityUsageResponse currentUsage = usage.findUsage(userId);
        return PetitionSimilarityResponse.of(
                properties.getThreshold(),
                cacheHit,
                currentUsage.remaining(),
                results
        );
    }

    public PetitionSimilarityUsageResponse findUsage(Long userId) {
        if (!users.existsByIdAndDeletedFalseAndLoginBannedFalse(userId)) {
            throw new PetitionSimilarityException(
                    PetitionSimilarityException.Reason.USER_NOT_FOUND);
        }
        return usage.findUsage(userId);
    }

    private void validateUsage(Long userId, LocalDateTime now, LocalDateTime windowStart) {
        long used = searchLogs.countByUserIdAndCountedTrueAndCreatedAtAfter(userId, windowStart);
        if (used < properties.getSearchLimit()) {
            return;
        }
        LocalDateTime nextAvailableAt = searchLogs
                .findOldestCountedCreatedAtAfter(userId, windowStart)
                .map(createdAt -> createdAt.plus(properties.getSearchWindow()))
                .orElse(now);
        long retryAfterSeconds = Math.max(0L,
                Duration.between(now, nextAvailableAt)
                        .plusSeconds(1).minusNanos(1).toSeconds());
        throw new PetitionSimilarityException(
                PetitionSimilarityException.Reason.RATE_LIMIT_EXCEEDED,
                retryAfterSeconds);
    }

    private float[] createQueryEmbedding(String text) {
        try {
            EmbeddingResult result = embeddingClient.embed(text);
            if (!result.modelName().equals(openAiProperties.getEmbeddingModel())
                    || result.dimensions() != openAiProperties.getEmbeddingDimensions()) {
                throw new EmbeddingClientException("embedding response model mismatch");
            }
            return result.vector();
        } catch (EmbeddingClientException exception) {
            throw new PetitionSimilarityException(
                    PetitionSimilarityException.Reason.AI_UNAVAILABLE);
        }
    }

    private List<SimilarPetitionResponse> search(
            float[] queryVector,
            String modelName,
            int dimensions,
            LocalDateTime now
    ) {
        return embeddings.findPublicReadyCandidates(
                        PetitionEmbeddingStatus.READY, modelName, dimensions)
                .stream()
                .filter(embedding -> isCurrentEmbedding(embedding, modelName, dimensions))
                .map(embedding -> toSimilarPetition(embedding, queryVector, now))
                .filter(response -> response.similarity() >= properties.getThreshold())
                .sorted(Comparator
                        .comparingDouble(SimilarPetitionResponse::similarity).reversed()
                        .thenComparing(SimilarPetitionResponse::id))
                .toList();
    }

    private boolean isCurrentEmbedding(
            PetitionEmbedding embedding,
            String modelName,
            int dimensions
    ) {
        String currentHash = PetitionContentHasher.sha256(
                PetitionEmbeddingText.from(embedding.getPetition()));
        return embedding.isReadyFor(modelName, dimensions, currentHash);
    }

    private SimilarPetitionResponse toSimilarPetition(
            PetitionEmbedding embedding,
            float[] queryVector,
            LocalDateTime now
    ) {
        Petition petition = embedding.getPetition();
        double similarity = EmbeddingVectorCodec.cosineSimilarity(
                queryVector, EmbeddingVectorCodec.decode(embedding.getEmbedding()));
        return SimilarPetitionResponse.from(
                petition, calculateEffectiveStatus(petition, now), similarity);
    }

    private PetitionStatus calculateEffectiveStatus(Petition petition, LocalDateTime now) {
        if (petition.getStatus() == PetitionStatus.OPEN
                && !petition.getAgreementDeadline().isAfter(now)
                && petition.getAgreementCount() < petition.getTargetAgreementCount()) {
            return PetitionStatus.EXPIRED;
        }
        return petition.getStatus();
    }
}
