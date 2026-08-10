package org.skhuconnect.report.dto;
import org.springframework.data.domain.Page;
import java.util.List;
public record ReportPageResponse(List<ReportResponse> content,int page,int size,long totalElements,int totalPages){
 public static ReportPageResponse from(Page<ReportResponse> p){return new ReportPageResponse(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());}
}