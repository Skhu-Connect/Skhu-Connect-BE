package org.skhuconnect.admin.content.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.content.dto.AdminCommentResponse;
import org.skhuconnect.admin.content.dto.AdminPageResponse;
import org.skhuconnect.admin.content.dto.AdminPetitionResponse;
import org.skhuconnect.admin.content.exception.AdminContentExceptionHandler;
import org.skhuconnect.admin.content.service.AdminContentService;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminContentControllerTest {

    private AdminContentService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AdminContentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminContentController(service))
                .setControllerAdvice(new AdminContentExceptionHandler())
                .build();
    }

    @Test
    void listsAndHidesPetitionWithAuthenticatedAdmin() throws Exception {
        when(service.findPetitions(0, 20)).thenReturn(new AdminPageResponse<>(
                List.of(petition()), 0, 20, 1, 1, true, true));
        when(service.hidePetition(eq(7L), eq(10L), any())).thenReturn(petition());

        mockMvc.perform(get("/connect/admin/petitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("title"));
        mockMvc.perform(patch("/connect/admin/petitions/10/hide")
                        .requestAttr("adminId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hiddenReason\":\"policy violation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void restoresPetition() throws Exception {
        // @PathVariable Long id 가 URL 템플릿의 {petitionId} 와 이름이 안 맞아 런타임에
        // MissingPathVariableException 으로 500 이 나던 자리 - 경로 바인딩을 실제로 태워서 잡는다.
        when(service.restorePetition(7L, 10L)).thenReturn(petition());

        mockMvc.perform(patch("/connect/admin/petitions/10/restore").requestAttr("adminId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void listsAndRestoresCommentOrReply() throws Exception {
        when(service.findComments(10L, 0, 20)).thenReturn(new AdminPageResponse<>(
                List.of(comment()), 0, 20, 1, 1, true, true));
        when(service.restoreComment(7L, 10L, 20L)).thenReturn(comment());

        mockMvc.perform(get("/connect/admin/petitions/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].parentCommentId").value(11));
        mockMvc.perform(patch("/connect/admin/petitions/10/comments/20/restore").requestAttr("adminId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void hideReasonIsValidated() throws Exception {
        mockMvc.perform(patch("/connect/admin/petitions/10/hide")
                        .requestAttr("adminId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hiddenReason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private AdminPetitionResponse petition() {
        return new AdminPetitionResponse(10L, 99L, false, PetitionCategory.FACILITY, PetitionStatus.OPEN,
                "title", "content", 0, 10, true, "policy violation", 7L,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }

    private AdminCommentResponse comment() {
        return new AdminCommentResponse(20L, 11L, 99L, false, "reply", 1, true,
                "policy violation", 7L, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }
}