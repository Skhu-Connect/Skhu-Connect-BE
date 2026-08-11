package org.skhuconnect.department.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.department.exception.UserDepartmentException;
import org.skhuconnect.department.exception.UserDepartmentExceptionHandler;
import org.skhuconnect.department.service.UserDepartmentService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserDepartmentControllerTest {
    private UserDepartmentService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(UserDepartmentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserDepartmentController(service))
                .setControllerAdvice(new UserDepartmentExceptionHandler())
                .build();
    }

    @Test
    void updateDepartmentReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/connect/users/me/department")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":2}"))
                .andExpect(status().isNoContent());

        verify(service).updateDepartment(1L, 2L);
    }

    @Test
    void updateDepartmentReturnsNotFoundForUnknownDepartment() throws Exception {
        doThrow(new UserDepartmentException(
                UserDepartmentException.Reason.DEPARTMENT_NOT_FOUND))
                .when(service).updateDepartment(1L, 999L);

        mockMvc.perform(patch("/connect/users/me/department")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("학과를 찾을 수 없습니다"));
    }

    @Test
    void updateDepartmentRejectsMissingDepartmentId() throws Exception {
        mockMvc.perform(patch("/connect/users/me/department")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
