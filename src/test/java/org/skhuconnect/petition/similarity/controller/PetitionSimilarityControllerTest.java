package org.skhuconnect.petition.similarity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.similarity.dto.PetitionSimilarityResponse;
import org.skhuconnect.petition.similarity.dto.PetitionSimilarityUsageResponse;
import org.skhuconnect.petition.similarity.dto.SimilarPetitionResponse;
import org.skhuconnect.petition.similarity.exception.PetitionSimilarityException;
import org.skhuconnect.petition.similarity.exception.PetitionSimilarityExceptionHandler;
import org.skhuconnect.petition.similarity.service.PetitionSimilarityService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PetitionSimilarityControllerTest {

    private PetitionSimilarityService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PetitionSimilarityService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PetitionSimilarityController(service))
                .setControllerAdvice(new PetitionSimilarityExceptionHandler())
                .build();
    }

    @Test
    void findSimilarReturnsResults() throws Exception {
        when(service.findSimilar(eq(1L), any())).thenReturn(new PetitionSimilarityResponse(
                0.75,
                1,
                false,
                2,
                List.of(similarPetition())
        ));

        mockMvc.perform(post("/connect/petitions/similar")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "시설 개선",
                                  "content": "학생 시설을 개선해주세요."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threshold").value(0.75))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.cached").value(false))
                .andExpect(jsonPath("$.remainingSearches").value(2))
                .andExpect(jsonPath("$.results[0].id").value(10))
                .andExpect(jsonPath("$.results[0].similarity").value(0.91));
    }

    @Test
    void invalidRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/connect/petitions/similar")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rateLimitReturnsTooManyRequestsProblemDetail() throws Exception {
        doThrow(new PetitionSimilarityException(
                PetitionSimilarityException.Reason.RATE_LIMIT_EXCEEDED, 120L))
                .when(service).findSimilar(eq(1L), any());

        mockMvc.perform(post("/connect/petitions/similar")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "시설 개선",
                                  "content": "학생 시설을 개선해주세요."
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title")
                        .value("Similar petition search rate limit exceeded"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(120));
    }

    @Test
    void aiFailureReturnsServiceUnavailableProblemDetail() throws Exception {
        doThrow(new PetitionSimilarityException(
                PetitionSimilarityException.Reason.AI_UNAVAILABLE))
                .when(service).findSimilar(eq(1L), any());

        mockMvc.perform(post("/connect/petitions/similar")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "시설 개선",
                                  "content": "학생 시설을 개선해주세요."
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title")
                        .value("AI similarity search is unavailable"));
    }

    @Test
    void findUsageReturnsRemainingCountAndRetryTime() throws Exception {
        when(service.findUsage(1L)).thenReturn(new PetitionSimilarityUsageResponse(
                3,
                3,
                0,
                600,
                120L,
                LocalDateTime.of(2026, 8, 5, 12, 2)
        ));

        mockMvc.perform(get("/connect/petitions/similar/usage")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(3))
                .andExpect(jsonPath("$.used").value(3))
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.retryAfterSeconds").value(120));
    }

    @Test
    void exposesSwaggerDocumentation() throws Exception {
        assertThat(PetitionSimilarityController.class).hasAnnotation(Tag.class);
        assertThat(PetitionSimilarityController.class.getMethod(
                        "findSimilar", Long.class,
                        org.skhuconnect.petition.similarity.dto.PetitionSimilarityRequest.class)
                .getAnnotation(Operation.class)).isNotNull();
        assertThat(PetitionSimilarityController.class.getMethod(
                        "findUsage", Long.class)
                .getAnnotation(Operation.class)).isNotNull();
    }

    private SimilarPetitionResponse similarPetition() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        return new SimilarPetitionResponse(
                10L,
                PetitionCategory.FACILITY,
                PetitionStatus.OPEN,
                "시설 개선",
                "학생 시설을 개선해주세요.",
                1,
                10,
                now.plusDays(30),
                now,
                now,
                0.91
        );
    }
}
