package org.skhuconnect.petition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.petition.dto.request.PetitionCreateRequest;
import org.skhuconnect.petition.dto.request.PetitionUpdateRequest;
import org.skhuconnect.admin.answer.entity.AnswerSource;
import org.skhuconnect.petition.dto.response.OfficialAnswerDetailResponse;
import org.skhuconnect.petition.dto.response.PetitionPageResponse;
import org.skhuconnect.petition.dto.response.PetitionQueryResponse;
import org.skhuconnect.petition.dto.response.PetitionResponse;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.skhuconnect.petition.exception.PetitionException;
import org.skhuconnect.petition.exception.PetitionExceptionHandler;
import org.skhuconnect.petition.service.PetitionService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PetitionControllerTest {

    private PetitionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PetitionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PetitionController(service))
                .setControllerAdvice(new PetitionExceptionHandler())
                .build();
    }

    @Test
    void createReturnsCreatedPetition() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(response("시설 개선", "내용"));

        mockMvc.perform(post("/connect/petitions")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "FACILITY",
                                  "title": "시설 개선",
                                  "content": "내용"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("FACILITY"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.title").value("시설 개선"));
    }

    @Test
    void invalidCreateRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/connect/petitions")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": null,
                                  "title": "",
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsUpdatedPetition() throws Exception {
        when(service.update(eq(1L), eq(10L), any()))
                .thenReturn(response("수정 제목", "수정 내용"));

        mockMvc.perform(put("/connect/petitions/10")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정 제목",
                                  "content": "수정 내용"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 제목"))
                .andExpect(jsonPath("$.content").value("수정 내용"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/connect/petitions/10")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(1L, 10L);
    }

    @Test
    void differentWriterReturnsForbiddenProblemDetail() throws Exception {
        doThrow(new PetitionException(PetitionException.Reason.PETITION_FORBIDDEN))
                .when(service).update(eq(1L), eq(10L), any());

        mockMvc.perform(put("/connect/petitions/10")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정 제목",
                                  "content": "수정 내용"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Petition access forbidden"));
    }

    @Test
    void nonEditablePetitionReturnsConflictProblemDetail() throws Exception {
        doThrow(new PetitionException(PetitionException.Reason.PETITION_NOT_EDITABLE))
                .when(service).delete(1L, 10L);

        mockMvc.perform(delete("/connect/petitions/10")
                        .requestAttr("userId", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Petition is not editable"));
    }

    @Test
    void missingPetitionReturnsNotFoundProblemDetail() throws Exception {
        doThrow(new PetitionException(PetitionException.Reason.PETITION_NOT_FOUND))
                .when(service).delete(1L, 10L);

        mockMvc.perform(delete("/connect/petitions/10")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Petition not found"));
    }

    @Test
    void anonymousUserCanQueryPetitionList() throws Exception {
        when(service.findAll(any())).thenReturn(pageResponse());

        mockMvc.perform(get("/connect/petitions")
                        .param("keyword", "library")
                        .param("category", "LIBRARY")
                        .param("status", "OPEN")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test
    void anonymousUserCanQueryPetitionDetail() throws Exception {
        when(service.findDetail(10L)).thenReturn(queryResponse());

        mockMvc.perform(get("/connect/petitions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.officialAnswer").doesNotExist());
    }

    @Test
    void answeredPetitionDetailIncludesOfficialAnswer() throws Exception {
        when(service.findDetail(10L)).thenReturn(queryResponseWithOfficialAnswer());

        mockMvc.perform(get("/connect/petitions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.officialAnswer.content").value("official answer"))
                .andExpect(jsonPath("$.officialAnswer.answerSource").value("SCHOOL_OFFICIAL"));
    }

    @Test
    void invalidSortReturnsBadRequest() throws Exception {
        doThrow(new PetitionException(PetitionException.Reason.INVALID_SORT))
                .when(service).findAll(any());

        mockMvc.perform(get("/connect/petitions")
                        .param("sort", "title,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title")
                        .value("Invalid petition sort property"));
    }

    @Test
    void hiddenDeletedOrMissingDetailReturnsNotFound() throws Exception {
        doThrow(new PetitionException(PetitionException.Reason.PETITION_NOT_FOUND))
                .when(service).findDetail(10L);

        mockMvc.perform(get("/connect/petitions/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Petition not found"));
    }
    @Test
    void invalidCategoryAndStatusReturnBadRequest() throws Exception {
        mockMvc.perform(get("/connect/petitions")
                        .param("category", "INVALID"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/connect/petitions")
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void defaultAndInvalidPageParametersAreHandled() throws Exception {
        when(service.findAll(any())).thenReturn(pageResponse());

        mockMvc.perform(get("/connect/petitions"))
                .andExpect(status().isOk());

        doThrow(new PetitionException(PetitionException.Reason.INVALID_PAGE))
                .when(service).findAll(any());
        mockMvc.perform(get("/connect/petitions")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title")
                        .value("Invalid petition page request"));
    }
    @Test
    void exposesSwaggerDocumentation() throws Exception {
        assertThat(PetitionController.class).hasAnnotation(Tag.class);
        assertThat(PetitionController.class.getMethod(
                "create", Long.class, PetitionCreateRequest.class)
                .getAnnotation(Operation.class)).isNotNull();
        assertThat(PetitionController.class.getMethod(
                "update", Long.class, Long.class, PetitionUpdateRequest.class)
                .getAnnotation(Operation.class)).isNotNull();
        assertThat(PetitionController.class.getMethod(
                "delete", Long.class, Long.class)
                .getAnnotation(Operation.class)).isNotNull();
    }

    private PetitionPageResponse pageResponse() {
        return new PetitionPageResponse(
                List.of(queryResponse()), 0, 20, 1, 1, true, true);
    }

    private PetitionQueryResponse queryResponse() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        return new PetitionQueryResponse(
                10L,
                PetitionCategory.LIBRARY,
                PetitionStatus.OPEN,
                "library",
                "content",
                0,
                10,
                now.plusDays(30),
                now,
                now,
                null
        );
    }
    private PetitionQueryResponse queryResponseWithOfficialAnswer() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        return new PetitionQueryResponse(
                10L, PetitionCategory.LIBRARY, PetitionStatus.ANSWERED,
                "library", "content", 10, 10, now.plusDays(30), now, now,
                new OfficialAnswerDetailResponse("official answer", AnswerSource.SCHOOL_OFFICIAL, now, now)
        );
    }

    private PetitionResponse response(String title, String content) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        return new PetitionResponse(
                10L,
                PetitionCategory.FACILITY,
                PetitionStatus.OPEN,
                title,
                content,
                0,
                13,
                now.plusDays(30),
                now,
                now
        );
    }
}
