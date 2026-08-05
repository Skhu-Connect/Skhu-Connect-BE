package org.skhuconnect.petition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.petition.dto.request.PetitionCreateRequest;
import org.skhuconnect.petition.dto.request.PetitionUpdateRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
