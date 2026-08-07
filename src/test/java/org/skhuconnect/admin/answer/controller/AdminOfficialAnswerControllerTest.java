package org.skhuconnect.admin.answer.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.answer.dto.OfficialAnswerResponse;
import org.skhuconnect.admin.answer.entity.AnswerSource;
import org.skhuconnect.admin.answer.exception.AdminOfficialAnswerExceptionHandler;
import org.skhuconnect.admin.answer.service.AdminOfficialAnswerService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminOfficialAnswerControllerTest {

    private AdminOfficialAnswerService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AdminOfficialAnswerService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOfficialAnswerController(service))
                .setControllerAdvice(new AdminOfficialAnswerExceptionHandler())
                .build();
    }

    @Test
    void registersAndUpdatesAnswerWithAuthenticatedAdmin() throws Exception {
        when(service.register(eq(7L), eq(10L), any())).thenReturn(response("registered"));
        when(service.update(eq(7L), eq(10L), any())).thenReturn(response("updated"));

        mockMvc.perform(post("/connect/admin/petitions/10/answer")
                        .requestAttr("adminId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"registered\",\"answerSource\":\"SCHOOL_OFFICIAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petitionId").value(10))
                .andExpect(jsonPath("$.answerSource").value("SCHOOL_OFFICIAL"));
        mockMvc.perform(put("/connect/admin/petitions/10/answer")
                        .requestAttr("adminId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"updated\",\"answerSource\":\"OPERATION_TEAM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("updated"));
    }

    @Test
    void validatesAnswerContentAndSource() throws Exception {
        mockMvc.perform(post("/connect/admin/petitions/10/answer")
                        .requestAttr("adminId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\",\"answerSource\":null}"))
                .andExpect(status().isBadRequest());
    }

    private OfficialAnswerResponse response(String content) {
        return new OfficialAnswerResponse(1L, 10L, 7L, content, AnswerSource.SCHOOL_OFFICIAL,
                LocalDateTime.now(), LocalDateTime.now());
    }
}