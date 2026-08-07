package org.skhuconnect.admin.answer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.admin.answer.dto.OfficialAnswerRequest;
import org.skhuconnect.admin.answer.dto.OfficialAnswerResponse;
import org.skhuconnect.admin.answer.service.AdminOfficialAnswerService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Official Answer", description = "Administrator official petition answer API")
@RestController
@RequestMapping("/connect/admin/petitions/{petitionId}/answer")
public class AdminOfficialAnswerController {

    private final AdminOfficialAnswerService service;

    public AdminOfficialAnswerController(AdminOfficialAnswerService service) {
        this.service = service;
    }

    @Operation(summary = "Register an official answer for an under-review petition")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Official answer registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Administrator or petition not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Answer already exists or petition state conflict", content = @Content)
    })
    @PostMapping
    public OfficialAnswerResponse register(
            @RequestAttribute("adminId") Long adminId,
            @PathVariable Long petitionId,
            @Valid @RequestBody OfficialAnswerRequest request
    ) {
        return service.register(adminId, petitionId, request);
    }

    @Operation(summary = "Update an official answer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Official answer updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Administrator, petition, or official answer not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Petition state conflict", content = @Content)
    })
    @PutMapping
    public OfficialAnswerResponse update(
            @RequestAttribute("adminId") Long adminId,
            @PathVariable Long petitionId,
            @Valid @RequestBody OfficialAnswerRequest request
    ) {
        return service.update(adminId, petitionId, request);
    }
}