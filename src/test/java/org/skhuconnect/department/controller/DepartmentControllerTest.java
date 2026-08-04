package org.skhuconnect.department.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.department.dto.response.DepartmentResponse;
import org.skhuconnect.department.service.DepartmentService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepartmentControllerTest {

    private DepartmentService departmentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        departmentService = mock(DepartmentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new DepartmentController(departmentService)
        ).build();
    }

    @Test
    void getDepartmentsReturnsDepartmentResponsesWithoutAuthentication() throws Exception {
        when(departmentService.getDepartments()).thenReturn(List.of(
                new DepartmentResponse(1L, "SOFTWARE", "소프트웨어융합학부")
        ));

        mockMvc.perform(get("/connect/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("SOFTWARE"))
                .andExpect(jsonPath("$[0].name").value("소프트웨어융합학부"));
    }

    @Test
    void getDepartmentsReturnsEmptyArrayWhenNoDepartmentExists() throws Exception {
        when(departmentService.getDepartments()).thenReturn(List.of());

        mockMvc.perform(get("/connect/departments"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
