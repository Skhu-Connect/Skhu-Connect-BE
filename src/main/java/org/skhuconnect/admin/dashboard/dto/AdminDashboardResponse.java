package org.skhuconnect.admin.dashboard.dto;
import org.skhuconnect.petition.entity.PetitionStatus; import org.skhuconnect.report.entity.ReportStatus; import java.util.Map;
public record AdminDashboardResponse(long totalUsers,long totalPetitions,Map<PetitionStatus,Long> petitionsByStatus,long totalComments,Map<ReportStatus,Long> reportsByStatus) {}
