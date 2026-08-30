package org.skhuconnect.report.service;
import org.skhuconnect.admin.entity.Admin; import org.skhuconnect.admin.repository.AdminRepository; import org.skhuconnect.comment.entity.Comment; import org.skhuconnect.comment.repository.CommentRepository; import org.skhuconnect.petition.entity.Petition; import org.skhuconnect.petition.repository.PetitionRepository; import org.skhuconnect.report.dto.*; import org.skhuconnect.report.entity.*; import org.skhuconnect.report.exception.ReportException; import org.skhuconnect.report.repository.ReportRepository; import org.skhuconnect.user.entity.User; import org.skhuconnect.user.repository.UserRepository; import org.springframework.data.domain.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.*;
@Service
public class ReportService{
 private final ReportRepository reports; private final UserRepository users; private final PetitionRepository petitions; private final CommentRepository comments; private final AdminRepository admins; private final Clock clock;
 public ReportService(ReportRepository r,UserRepository u,PetitionRepository p,CommentRepository c,AdminRepository a,Clock cl){reports=r;users=u;petitions=p;comments=c;admins=a;clock=cl;}
 @Transactional public ReportResponse create(Long userId,ReportCreateRequest q){if((q.petitionId()==null)==(q.commentId()==null))throw e(ReportException.Reason.INVALID_TARGET); User u=users.findById(userId).orElseThrow(()->e(ReportException.Reason.PETITION_NOT_FOUND)); Report r;
  if(q.petitionId()!=null){Petition p=petitions.findByIdAndDeletedFalseAndHiddenFalse(q.petitionId()).orElseThrow(()->e(ReportException.Reason.PETITION_NOT_FOUND)); if(p.isWrittenBy(userId))throw e(ReportException.Reason.SELF_REPORT); if(reports.existsByReporterIdAndPetitionId(userId,p.getId()))throw e(ReportException.Reason.ALREADY_REPORTED); r=Report.forPetition(u,p,q.reasonType(),q.reasonDetail());}
  else {Comment c=comments.findById(q.commentId()).orElseThrow(()->e(ReportException.Reason.COMMENT_NOT_FOUND)); if(c.isDeleted()||c.isHidden()||c.getPetition().isDeleted()||c.getPetition().isHidden())throw e(ReportException.Reason.COMMENT_NOT_FOUND); if(c.isWrittenBy(userId))throw e(ReportException.Reason.SELF_REPORT); if(reports.existsByReporterIdAndCommentId(userId,c.getId()))throw e(ReportException.Reason.ALREADY_REPORTED); r=Report.forComment(u,c,q.reasonType(),q.reasonDetail());}
  return ReportResponse.from(reports.save(r));}
 @Transactional(readOnly=true) public ReportPageResponse list(ReportStatus s,ReportTargetType t,int page,int size){if(page<0||size<1||size>100)throw e(ReportException.Reason.INVALID_PAGE);Pageable p=PageRequest.of(page,size,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")));Page<Report> x=s!=null&&t!=null?reports.findByStatusAndTargetType(s,t,p):s!=null?reports.findByStatus(s,p):t!=null?reports.findByTargetType(t,p):reports.findAll(p);return ReportPageResponse.from(x.map(ReportResponse::from));}
 @Transactional public ReportResponse process(Long adminId,Long reportId,ReportProcessRequest q){if(q.status()==ReportStatus.PENDING)throw e(ReportException.Reason.INVALID_STATUS); Report r=reports.findById(reportId).orElseThrow(()->e(ReportException.Reason.REPORT_NOT_FOUND)); Admin a=admins.findById(adminId).orElseThrow(()->e(ReportException.Reason.REPORT_NOT_FOUND)); if(r.getStatus()!=ReportStatus.PENDING)throw e(ReportException.Reason.INVALID_STATUS); if(q.status()==ReportStatus.ACTION_TAKEN)hideTarget(r,a,q.processingReason()); r.process(q.status(),a,q.processingReason(),LocalDateTime.now(clock));return ReportResponse.from(r);}
 /**
  * 조치함 처리 시 대상을 숨긴다. 이미 숨겨졌거나 작성자가 삭제한 대상은 건너뛴다 -
  * 삭제된 대상은 이미 노출이 끊겨 있어 숨김이 무의미하고, hide() 가 IllegalStateException 을
  * 던져 신고를 영원히 PENDING 으로 남기기 때문이다. 관리자는 사유를 남기고 종결할 수 있어야 한다.
  */
 private void hideTarget(Report r,Admin a,String reason){
  if(r.getTargetType()==ReportTargetType.PETITION){Petition p=r.getPetition(); if(!p.isHidden()&&!p.isDeleted())p.hide(reason,a,LocalDateTime.now(clock));}
  else{Comment c=r.getComment(); if(!c.isHidden()&&!c.isDeleted())c.hide(reason,a,LocalDateTime.now(clock));}
 }
 private ReportException e(ReportException.Reason r){return new ReportException(r);}
}