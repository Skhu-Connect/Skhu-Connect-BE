package org.skhuconnect.agreement.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.agreement.dto.response.AgreementResponse;
import org.skhuconnect.agreement.exception.AgreementException;
import org.skhuconnect.agreement.exception.AgreementExceptionHandler;
import org.skhuconnect.agreement.service.AgreementService;
import org.skhuconnect.petition.entity.PetitionStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgreementControllerTest {

    private AgreementService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AgreementService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AgreementController(service))
                .setControllerAdvice(new AgreementExceptionHandler())
                .build();
    }

    @Test
    void agreeReturnsCreatedAndUpdatedPetitionState() throws Exception {
        when(service.agree(1L, 10L)).thenReturn(new AgreementResponse(
                10L, 5, PetitionStatus.UNDER_REVIEW));

        mockMvc.perform(post("/connect/petitions/10/agreements")
                        .requestAttr("userId", 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petitionId").value(10))
                .andExpect(jsonPath("$.agreementCount").value(5))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test
    void cancelReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/connect/petitions/10/agreements")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).cancel(1L, 10L);
    }

    @Test
    void duplicateAgreementReturnsConflict() throws Exception {
        doThrow(new AgreementException(
                AgreementException.Reason.AGREEMENT_DUPLICATE))
                .when(service).agree(1L, 10L);

        mockMvc.perform(post("/connect/petitions/10/agreements")
                        .requestAttr("userId", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Petition agreement already exists"));
    }

    @Test
    void missingPetitionReturnsNotFound() throws Exception {
        doThrow(new AgreementException(
                AgreementException.Reason.PETITION_NOT_FOUND))
                .when(service).cancel(1L, 10L);

        mockMvc.perform(delete("/connect/petitions/10/agreements")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Petition not found"));
    }
}