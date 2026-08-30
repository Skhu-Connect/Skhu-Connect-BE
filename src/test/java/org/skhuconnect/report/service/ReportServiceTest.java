package org.skhuconnect.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.repository.CommentRepository;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.skhuconnect.report.dto.ReportProcessRequest;
import org.skhuconnect.report.dto.ReportResponse;
import org.skhuconnect.report.entity.Report;
import org.skhuconnect.report.entity.ReportReasonType;
import org.skhuconnect.report.entity.ReportStatus;
import org.skhuconnect.report.repository.ReportRepository;
import org.skhuconnect.user.entity.User;
import org.skhuconnect.user.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 12, 0);

    private ReportRepository reports;
    private AdminRepository admins;
    private ReportService service;
    private Admin admin;

    @BeforeEach
    void setUp() {
        reports = mock(ReportRepository.class);
        admins = mock(AdminRepository.class);
        admin = mock(Admin.class);
        service = new ReportService(
                reports,
                mock(UserRepository.class),
                mock(PetitionRepository.class),
                mock(CommentRepository.class),
                admins,
                Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
        when(admins.findById(7L)).thenReturn(Optional.of(admin));
    }

    @Test
    void 삭제된_청원은_숨기지_않고_신고만_종결한다() {
        Petition petition = mock(Petition.class);
        when(petition.isDeleted()).thenReturn(true);
        when(petition.getTitle()).thenReturn("신고당한 청원");
        when(petition.getContent()).thenReturn("작성자가 지운 원문");
        Report report = petitionReport(petition);

        ReportResponse response = service.process(7L, 1L, actionTaken());

        verify(petition, never()).hide(anyString(), any(), any());
        assertThat(report.getStatus()).isEqualTo(ReportStatus.ACTION_TAKEN);
        assertThat(response.targetDeleted()).isTrue();
        // 3번의 핵심 - 삭제된 뒤에도 관리자는 신고 대상 원문을 볼 수 있어야 한다.
        assertThat(response.targetTitle()).isEqualTo("신고당한 청원");
        assertThat(response.targetContent()).isEqualTo("작성자가 지운 원문");
    }

    @Test
    void 삭제된_댓글도_숨기지_않고_신고만_종결한다() {
        Petition petition = mock(Petition.class);
        when(petition.getId()).thenReturn(42L);
        Comment comment = mock(Comment.class);
        when(comment.isDeleted()).thenReturn(true);
        when(comment.getPetition()).thenReturn(petition);
        when(comment.getContent()).thenReturn("작성자가 지운 댓글");
        Report report = commentReport(comment);

        ReportResponse response = service.process(7L, 1L, actionTaken());

        verify(comment, never()).hide(anyString(), any(), any());
        assertThat(report.getStatus()).isEqualTo(ReportStatus.ACTION_TAKEN);
        assertThat(response.targetDeleted()).isTrue();
        assertThat(response.targetContent()).isEqualTo("작성자가 지운 댓글");
        // 댓글 신고는 report.petition 이 null 이라 그동안 petitionId 가 비어 있었다.
        assertThat(response.petitionId()).isEqualTo(42L);
    }

    @Test
    void 살아있는_청원은_조치함_처리에서_숨겨진다() {
        Petition petition = mock(Petition.class);
        Report report = petitionReport(petition);

        service.process(7L, 1L, actionTaken());

        verify(petition).hide("욕설", admin, NOW);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.ACTION_TAKEN);
    }

    private Report petitionReport(Petition petition) {
        Report report = Report.forPetition(
                mock(User.class), petition, ReportReasonType.ABUSE, "욕설이 포함된 글이라 신고합니다.");
        when(reports.findById(1L)).thenReturn(Optional.of(report));
        return report;
    }

    private Report commentReport(Comment comment) {
        Report report = Report.forComment(
                mock(User.class), comment, ReportReasonType.ABUSE, "욕설이 포함된 글이라 신고합니다.");
        when(reports.findById(1L)).thenReturn(Optional.of(report));
        return report;
    }

    private ReportProcessRequest actionTaken() {
        return new ReportProcessRequest(ReportStatus.ACTION_TAKEN, "욕설");
    }
}
