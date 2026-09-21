package org.skhuconnect.petition.similarity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.similarity.client.EmbeddingClient;
import org.skhuconnect.petition.similarity.client.EmbeddingClientException;
import org.skhuconnect.petition.similarity.client.EmbeddingResult;
import org.skhuconnect.petition.similarity.config.OpenAiProperties;
import org.skhuconnect.petition.similarity.config.PetitionSimilarityProperties;
import org.skhuconnect.petition.similarity.dto.PetitionSimilarityRequest;
import org.skhuconnect.petition.similarity.dto.PetitionSimilarityResponse;
import org.skhuconnect.petition.similarity.entity.PetitionEmbedding;
import org.skhuconnect.petition.similarity.entity.PetitionSimilaritySearchLog;
import org.skhuconnect.petition.similarity.exception.PetitionSimilarityException;
import org.skhuconnect.petition.similarity.repository.PetitionEmbeddingRepository;
import org.skhuconnect.petition.similarity.repository.PetitionSimilaritySearchLogRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PetitionSimilarityServiceTest {

    private UserRepository users;
    private PetitionEmbeddingRepository embeddings;
    private PetitionSimilaritySearchLogRepository searchLogs;
    private EmbeddingClient embeddingClient;
    private PetitionSimilarityService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        embeddings = mock(PetitionEmbeddingRepository.class);
        searchLogs = mock(PetitionSimilaritySearchLogRepository.class);
        embeddingClient = mock(EmbeddingClient.class);
        OpenAiProperties openAiProperties = new OpenAiProperties();
        PetitionSimilarityProperties properties = new PetitionSimilarityProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-08-05T03:00:00Z"),
                ZoneId.of("Asia/Seoul"));
        PetitionSimilarityUsage usage = new PetitionSimilarityUsage(
                searchLogs, properties, clock);
        service = new PetitionSimilarityService(
                users,
                embeddings,
                searchLogs,
                embeddingClient,
                openAiProperties,
                properties,
                usage,
                clock
        );
    }

    @Test
    void findsSimilarPetitionsSortedBySimilarityAndStoresCountedLog() {
        User user = activeUser(1L);
        Petition first = petition(10L, "시설 개선", "도서관 시설을 개선해주세요.", 0,
                LocalDateTime.of(2026, 9, 1, 12, 0));
        Petition second = petition(11L, "식당 개선", "학생 식당 메뉴를 개선해주세요.", 0,
                LocalDateTime.of(2026, 9, 1, 12, 0));
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(searchLogs.countByUserIdAndCountedTrueAndCreatedAtAfter(eq(1L), any()))
                .thenReturn(0L, 1L);
        when(embeddingClient.embed(any()))
                .thenReturn(new EmbeddingResult("text-embedding-3-small", 256,
                        vector(1.0f, 0.0f)));
        when(embeddings.findPublicReadyCandidates(any(), eq("text-embedding-3-small"), eq(256)))
                .thenReturn(List.of(
                        embedding(first, vector(0.8f, 0.6f)),
                        embedding(second, vector(0.95f, 0.05f))
                ));

        PetitionSimilarityResponse response = service.findSimilar(
                1L, new PetitionSimilarityRequest("개선 요청", "학생 시설 개선이 필요합니다."));

        assertThat(response.cached()).isFalse();
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.remainingSearches()).isEqualTo(2);
        assertThat(response.results()).extracting("id").containsExactly(11L, 10L);
        ArgumentCaptor<PetitionSimilaritySearchLog> captor =
                ArgumentCaptor.forClass(PetitionSimilaritySearchLog.class);
        verify(searchLogs).save(captor.capture());
        assertThat(captor.getValue().isCounted()).isTrue();
    }

    @Test
    void staleEmbeddingsAreExcluded() {
        User user = activeUser(1L);
        Petition petition = petition(10L, "이전 제목", "이전 내용", 0,
                LocalDateTime.of(2026, 9, 1, 12, 0));
        PetitionEmbedding stale = PetitionEmbedding.ready(
                petition,
                "text-embedding-3-small",
                256,
                PetitionContentHasher.sha256("old"),
                EmbeddingVectorCodec.encode(vector(1.0f, 0.0f)),
                LocalDateTime.of(2026, 8, 5, 12, 0)
        );
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(searchLogs.countByUserIdAndCountedTrueAndCreatedAtAfter(eq(1L), any()))
                .thenReturn(0L);
        when(embeddingClient.embed(any()))
                .thenReturn(new EmbeddingResult("text-embedding-3-small", 256,
                        vector(1.0f, 0.0f)));
        when(embeddings.findPublicReadyCandidates(any(), eq("text-embedding-3-small"), eq(256)))
                .thenReturn(List.of(stale));

        PetitionSimilarityResponse response = service.findSimilar(
                1L, new PetitionSimilarityRequest("새 제목", "새 내용"));

        assertThat(response.results()).isEmpty();
        verify(searchLogs).save(any());
    }

    @Test
    void sameContentWithinWindowReusesCachedEmbeddingWithoutChargingAgain() {
        User user = activeUser(1L);
        PetitionSimilaritySearchLog cached = PetitionSimilaritySearchLog.counted(
                user,
                PetitionContentHasher.sha256(PetitionEmbeddingText.from("제목", "내용")),
                "text-embedding-3-small",
                256,
                EmbeddingVectorCodec.encode(vector(1.0f, 0.0f))
        );
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(searchLogs
                .findTopByUserIdAndQueryHashAndModelNameAndDimensionsAndCreatedAtAfterOrderByCreatedAtDesc(
                        eq(1L), any(), eq("text-embedding-3-small"), eq(256), any()))
                .thenReturn(Optional.of(cached));
        when(embeddings.findPublicReadyCandidates(any(), eq("text-embedding-3-small"), eq(256)))
                .thenReturn(List.of());

        PetitionSimilarityResponse response = service.findSimilar(
                1L, new PetitionSimilarityRequest("제목", "내용"));

        assertThat(response.cached()).isTrue();
        verify(embeddingClient, never()).embed(any());
        verify(searchLogs, never()).save(any());
    }

    @Test
    void rateLimitExceededBeforeExternalAiCall() {
        User user = activeUser(1L);
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(searchLogs.countByUserIdAndCountedTrueAndCreatedAtAfter(eq(1L), any()))
                .thenReturn(3L);
        when(searchLogs.findOldestCountedCreatedAtAfter(eq(1L), any()))
                .thenReturn(Optional.of(LocalDateTime.of(2026, 8, 5, 11, 55)));

        assertThatThrownBy(() -> service.findSimilar(
                1L, new PetitionSimilarityRequest("제목", "내용")))
                .isInstanceOf(PetitionSimilarityException.class)
                .extracting("reason", "retryAfterSeconds")
                .containsExactly(
                        PetitionSimilarityException.Reason.RATE_LIMIT_EXCEEDED,
                        300L
                );
        verify(embeddingClient, never()).embed(any());
        verify(searchLogs, never()).save(any());
    }

    @Test
    void aiFailureReturnsServiceUnavailableReasonWithoutCharging() {
        User user = activeUser(1L);
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(searchLogs.countByUserIdAndCountedTrueAndCreatedAtAfter(eq(1L), any()))
                .thenReturn(0L);
        when(embeddingClient.embed(any()))
                .thenThrow(new EmbeddingClientException("down"));

        assertThatThrownBy(() -> service.findSimilar(
                1L, new PetitionSimilarityRequest("제목", "내용")))
                .isInstanceOf(PetitionSimilarityException.class)
                .extracting("reason")
                .isEqualTo(PetitionSimilarityException.Reason.AI_UNAVAILABLE);
        verify(searchLogs, never()).save(any());
    }

    private PetitionEmbedding embedding(Petition petition, float[] vector) {
        String hash = PetitionContentHasher.sha256(PetitionEmbeddingText.from(petition));
        return PetitionEmbedding.ready(
                petition,
                "text-embedding-3-small",
                256,
                hash,
                EmbeddingVectorCodec.encode(vector),
                LocalDateTime.of(2026, 8, 5, 12, 0)
        );
    }

    private Petition petition(
            Long id,
            String title,
            String content,
            int agreementCount,
            LocalDateTime agreementDeadline
    ) {
        Petition petition = Petition.create(
                mock(User.class),
                PetitionCategory.FACILITY,
                title,
                content,
                10,
                LocalDateTime.of(2026, 8, 1, 12, 0)
        );
        ReflectionTestUtils.setField(petition, "id", id);
        ReflectionTestUtils.setField(petition, "agreementCount", agreementCount);
        ReflectionTestUtils.setField(petition, "agreementDeadline", agreementDeadline);
        return petition;
    }

    private User activeUser(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.isLoginBanned()).thenReturn(false);
        return user;
    }

    private float[] vector(float first, float second) {
        float[] vector = new float[256];
        vector[0] = first;
        vector[1] = second;
        return vector;
    }
}
