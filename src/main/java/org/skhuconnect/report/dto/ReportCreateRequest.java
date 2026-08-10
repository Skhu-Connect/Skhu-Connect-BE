package org.skhuconnect.report.dto;
import jakarta.validation.constraints.*;
import org.skhuconnect.report.entity.ReportReasonType;
public record ReportCreateRequest(Long petitionId,Long commentId,@NotNull ReportReasonType reasonType,@NotBlank @Size(min=10,max=500) String reasonDetail){}