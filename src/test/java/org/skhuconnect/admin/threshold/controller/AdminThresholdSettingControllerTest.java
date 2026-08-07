package org.skhuconnect.admin.threshold.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.threshold.dto.AdminThresholdSettingResponse;
import org.skhuconnect.admin.threshold.dto.AdminThresholdSettingUpdateRequest;
import org.skhuconnect.admin.threshold.exception.AdminThresholdSettingException;
import org.skhuconnect.admin.threshold.exception.AdminThresholdSettingExceptionHandler;
import org.skhuconnect.admin.threshold.service.AdminThresholdSettingService;
import org.skhuconnect.petition.entity.PetitionCategory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminThresholdSettingControllerTest {

    private AdminThresholdSettingService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AdminThresholdSettingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminThresholdSettingController(service))
                .setControllerAdvice(new AdminThresholdSettingExceptionHandler())
                .build();
    }

    @Test
    void listReturnsCurrentSettings() throws Exception {
        when(service.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/connect/admin/threshold-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("FACILITY"))
                .andExpect(jsonPath("$[0].targetAgreementCount").value(10));
    }

    @Test
    void updateUsesAuthenticatedAdminAndValidatesRequest() throws Exception {
        when(service.update(eq(7L), eq(PetitionCategory.FACILITY), any()))
                .thenReturn(response());

        mockMvc.perform(put("/connect/admin/threshold-settings/FACILITY")
                        .requestAttr("adminId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalStudentCount":1000,"thresholdRate":0.0100,
                                "minimumCount":5,"changeReason":"annual enrollment update"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changeReason").value("annual enrollment update"));

        mockMvc.perform(put("/connect/admin/threshold-settings/FACILITY")
                        .requestAttr("adminId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalStudentCount":0,"thresholdRate":1.1000,
                                "minimumCount":0,"changeReason":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingSettingReturnsNotFound() throws Exception {
        when(service.update(eq(7L), eq(PetitionCategory.FACILITY), any()))
                .thenThrow(new AdminThresholdSettingException(
                        AdminThresholdSettingException.Reason.THRESHOLD_SETTING_NOT_FOUND));

        mockMvc.perform(put("/connect/admin/threshold-settings/FACILITY")
                        .requestAttr("adminId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalStudentCount":1000,"thresholdRate":0.0100,
                                "minimumCount":5,"changeReason":"reason"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Threshold setting not found"));
    }

    private AdminThresholdSettingResponse response() {
        return new AdminThresholdSettingResponse(PetitionCategory.FACILITY,
                1000, new BigDecimal("0.0100"), 5, 10,
                7L, "annual enrollment update", null);
    }
}