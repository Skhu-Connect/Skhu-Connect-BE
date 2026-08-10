package org.skhuconnect.report.dto;
import org.skhuconnect.report.entity.*;
import java.time.LocalDateTime;
public record ReportResponse(Long id,ReportStatus status,ReportTargetType targetType,Long petitionId,Long commentId,org.skhuconnect.report.entity.ReportReasonType reasonType,String reasonDetail,Long processedByAdminId,LocalDateTime processedAt,String processingReason){
 public static ReportResponse from(Report r){return new ReportResponse(r.getId(),r.getStatus(),r.getTargetType(),r.getPetition()==null?null:r.getPetition().getId(),r.getComment()==null?null:r.getComment().getId(),r.getReasonType(),r.getReasonDetail(),r.getProcessedByAdmin()==null?null:r.getProcessedByAdmin().getId(),r.getProcessedAt(),r.getProcessingReason());}
}