package org.skhuconnect.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.report.entity.Report;
import org.skhuconnect.report.entity.ReportActionType;
import org.skhuconnect.report.entity.ReportReasonType;
import org.skhuconnect.report.entity.ReportStatus;
import org.skhuconnect.report.entity.ReportTargetType;

import java.time.LocalDateTime;

@Schema(description = "신고 1건. 신고 대상의 원문을 함께 내려보내 작성자가 글을 삭제하거나 "
        + "관리자가 숨긴 뒤에도 신고 사유를 판단할 수 있게 한다.")
public record ReportResponse(
        Long id,
        ReportStatus status,
        ReportTargetType targetType,
        @Schema(description = "조치 종류. status가 ACTION_TAKEN일 때만 채워진다(HIDE: 대상 숨김, USER_LOGIN_BAN: 작성자 로그인 정지)")
        ReportActionType actionType,
        @Schema(description = "신고 대상 청원 id. 댓글 신고면 그 댓글이 달린 청원 id")
        Long petitionId,
        @Schema(description = "댓글 신고일 때만 채워진다")
        Long commentId,
        ReportReasonType reasonType,
        String reasonDetail,
        Long processedByAdminId,
        LocalDateTime processedAt,
        String processingReason,
        @Schema(description = "청원 신고면 청원 제목. 댓글 신고면 null")
        String targetTitle,
        @Schema(description = "신고 대상 원문. 청원 본문 또는 댓글 내용")
        String targetContent,
        @Schema(description = "대상을 작성자가 삭제했는지. true 면 사용자 화면에서는 이미 사라진 글이다")
        boolean targetDeleted,
        @Schema(description = "대상을 관리자가 숨겼는지")
        boolean targetHidden
) {
    public static ReportResponse from(Report report) {
        Petition petition = report.getPetition();
        Comment comment = report.getComment();
        Long petitionId = null;
        String targetTitle = null;
        String targetContent = null;
        boolean targetDeleted = false;
        boolean targetHidden = false;
        if (petition != null) {
            petitionId = petition.getId();
            targetTitle = petition.getTitle();
            targetContent = petition.getContent();
            targetDeleted = petition.isDeleted();
            targetHidden = petition.isHidden();
        } else if (comment != null) {
            // 댓글 신고는 report.petition 이 null 이다. 소속 청원 id 는 프록시에서 꺼내므로
            // 추가 쿼리가 나가지 않는다(식별자 접근은 프록시를 초기화하지 않는다).
            petitionId = comment.getPetition().getId();
            targetContent = comment.getContent();
            targetDeleted = comment.isDeleted();
            targetHidden = comment.isHidden();
        }
        return new ReportResponse(
                report.getId(),
                report.getStatus(),
                report.getTargetType(),
                report.getActionType(),
                petitionId,
                comment == null ? null : comment.getId(),
                report.getReasonType(),
                report.getReasonDetail(),
                report.getProcessedByAdmin() == null ? null : report.getProcessedByAdmin().getId(),
                report.getProcessedAt(),
                report.getProcessingReason(),
                targetTitle,
                targetContent,
                targetDeleted,
                targetHidden);
    }
}
