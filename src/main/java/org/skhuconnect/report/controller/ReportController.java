package org.skhuconnect.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.report.dto.ReportCreateRequest;
import org.skhuconnect.report.dto.ReportPageResponse;
import org.skhuconnect.report.dto.ReportProcessRequest;
import org.skhuconnect.report.dto.ReportResponse;
import org.skhuconnect.report.entity.ReportStatus;
import org.skhuconnect.report.entity.ReportTargetType;
import org.skhuconnect.report.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports", description = "청원·댓글 신고 API")
@RestController
@RequestMapping("/connect")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(
            summary = "청원 또는 댓글 신고",
            description = "petitionId 와 commentId 중 정확히 하나만 보냅니다. "
                    + "이미 삭제되거나 숨겨진 대상, 본인이 쓴 글은 신고할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 접수 성공"),
            @ApiResponse(responseCode = "400",
                    description = "대상 지정 오류 또는 사유 형식 오류(10~500자)", content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "대상 청원 또는 댓글 없음(삭제·숨김 포함)", content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "본인 글 신고 또는 같은 대상 중복 신고", content = @Content)
    })
    @PostMapping("/reports")
    public ReportResponse create(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        return reportService.create(userId, request);
    }

    @Operation(
            summary = "신고 목록 조회",
            description = "관리자 전용입니다. 각 신고는 대상 원문(targetTitle·targetContent)과 "
                    + "현재 상태(targetDeleted·targetHidden)를 함께 반환하므로, 작성자가 삭제한 글도 "
                    + "신고 검토 화면에서는 원문을 확인할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "페이지 값 오류", content = @Content)
    })
    @GetMapping("/admin/reports")
    public ReportPageResponse list(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.list(status, targetType, page, size);
    }

    @Operation(
            summary = "신고 처리",
            description = "관리자 전용입니다. ACTION_TAKEN 이면 대상을 숨김 처리합니다. "
                    + "다만 작성자가 이미 삭제한 대상은 숨기지 않고 신고만 종결합니다 - "
                    + "삭제된 글은 이미 노출이 끊겨 있기 때문입니다. "
                    + "PENDING 상태인 신고만 처리할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 처리 성공"),
            @ApiResponse(responseCode = "400",
                    description = "status 가 PENDING 이거나 처리 사유 형식 오류", content = @Content),
            @ApiResponse(responseCode = "404", description = "신고 또는 관리자 없음", content = @Content)
    })
    @PatchMapping("/admin/reports/{reportId}")
    public ReportResponse process(
            @Parameter(hidden = true) @RequestAttribute("adminId") Long adminId,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportProcessRequest request
    ) {
        return reportService.process(adminId, reportId, request);
    }
}
