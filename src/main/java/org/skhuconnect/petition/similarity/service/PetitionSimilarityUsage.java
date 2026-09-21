package org.skhuconnect.petition.similarity.service;

import org.skhuconnect.petition.similarity.config.PetitionSimilarityProperties;
import org.skhuconnect.petition.similarity.dto.PetitionSimilarityUsageResponse;
import org.skhuconnect.petition.similarity.repository.PetitionSimilaritySearchLogRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PetitionSimilarityUsage {

    private final PetitionSimilaritySearchLogRepository searchLogs;
    private final PetitionSimilarityProperties properties;
    private final Clock clock;

    public PetitionSimilarityUsage(
            PetitionSimilaritySearchLogRepository searchLogs,
            PetitionSimilarityProperties properties,
            Clock clock
    ) {
        this.searchLogs = searchLogs;
        this.properties = properties;
        this.clock = clock;
    }

    public PetitionSimilarityUsageResponse findUsage(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime windowStart = now.minus(properties.getSearchWindow());
        int used = Math.toIntExact(searchLogs.countByUserIdAndCountedTrueAndCreatedAtAfter(
                userId, windowStart));
        int remaining = Math.max(0, properties.getSearchLimit() - used);
        LocalDateTime nextAvailableAt = null;
        Long retryAfterSeconds = null;
        if (remaining == 0) {
            nextAvailableAt = searchLogs.findOldestCountedCreatedAtAfter(userId, windowStart)
                    .map(createdAt -> createdAt.plus(properties.getSearchWindow()))
                    .orElse(now);
            retryAfterSeconds = Math.max(0L,
                    Duration.between(now, nextAvailableAt)
                            .plusSeconds(1).minusNanos(1).toSeconds());
        }
        return new PetitionSimilarityUsageResponse(
                properties.getSearchLimit(),
                used,
                remaining,
                properties.getSearchWindow().toSeconds(),
                retryAfterSeconds,
                nextAvailableAt
        );
    }
}
