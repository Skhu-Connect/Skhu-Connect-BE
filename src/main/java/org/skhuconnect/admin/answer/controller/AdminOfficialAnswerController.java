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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "愿由ъ옄 怨듭떇 ?듬?", description = "愿由ъ옄 怨듭떇 ?듬? API")
@RestController
@RequestMapping("/connect/admin/petitions/{petitionId}/answer")
public class AdminOfficialAnswerController {

    private final AdminOfficialAnswerService service;

    public AdminOfficialAnswerController(AdminOfficialAnswerService service) {
        this.service = service;
    }

    @Operation(summary = "검토 중 청원 공식 답변 등록")
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

    @Operation(summary = "공식 답변 수정")
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

    @Operation(summary = "공식 답변 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Official answer retrieved"),
            @ApiResponse(responseCode = "404", description = "Administrator, petition, or official answer not found", content = @Content)
    })
    @GetMapping
    public OfficialAnswerResponse find(
            @RequestAttribute("adminId") Long adminId,
            @PathVariable Long petitionId
    ) {
        return service.find(adminId, petitionId);
    }
}