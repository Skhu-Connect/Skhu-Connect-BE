package org.skhuconnect.report.dto;
import jakarta.validation.constraints.*;
import org.skhuconnect.report.entity.ReportStatus;
public record ReportProcessRequest(@NotNull ReportStatus status,@NotBlank @Size(min=1,max=500) String processingReason){}