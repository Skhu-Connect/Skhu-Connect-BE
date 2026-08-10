package org.skhuconnect.report.controller;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.tags.Tag; import jakarta.validation.Valid; import org.skhuconnect.report.dto.*; import org.skhuconnect.report.entity.*; import org.skhuconnect.report.service.ReportService; import org.springframework.web.bind.annotation.*;
@Tag(name="Reports",description="청원·댓글 신고 API")
@RestController @RequestMapping("/connect")
public class ReportController{
 private final ReportService s; public ReportController(ReportService s){this.s=s;}
 @Operation(summary="청원 또는 댓글 신고")
 @PostMapping("/reports") public ReportResponse create(@RequestAttribute("userId") Long id,@Valid @RequestBody ReportCreateRequest q){return s.create(id,q);}
 @Operation(summary="신고 목록 조회")
 @GetMapping("/admin/reports") public ReportPageResponse list(@RequestParam(required=false) ReportStatus status,@RequestParam(required=false) ReportTargetType targetType,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return s.list(status,targetType,page,size);}
 @Operation(summary="신고 처리")
 @PatchMapping("/admin/reports/{reportId}") public ReportResponse process(@RequestAttribute("adminId") Long id,@PathVariable Long reportId,@Valid @RequestBody ReportProcessRequest q){return s.process(id,reportId,q);}
}
