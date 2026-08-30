package org.skhuconnect.report.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.global.entity.BaseEntity;
import org.skhuconnect.petition.entity.Petition;
import org.skhuconnect.user.entity.User;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name="reports", uniqueConstraints={
 @UniqueConstraint(name="uk_reports_user_petition", columnNames={"reporter_id","petition_id"}),
 @UniqueConstraint(name="uk_reports_user_comment", columnNames={"reporter_id","comment_id"})
})
public class Report extends BaseEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="reporter_id",nullable=false) private User reporter;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="petition_id") private Petition petition;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="comment_id") private Comment comment;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private ReportTargetType targetType;
 @Column(nullable=false,length=50) private ReportReasonType reasonType;
 @Column(nullable=false,length=500) private String reasonDetail;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private ReportStatus status;
 @Enumerated(EnumType.STRING) @Column(length=30) private ReportActionType actionType;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="processed_by_admin_id") private Admin processedByAdmin;
 @Column private LocalDateTime processedAt;
 @Column(length=500) private String processingReason;
 protected Report(){}
 private Report(User reporter, Petition petition, Comment comment, ReportTargetType targetType,ReportReasonType reasonType,String reasonDetail){
  this.reporter=Objects.requireNonNull(reporter); this.petition=petition; this.comment=comment; this.targetType=targetType;
  this.reasonType=Objects.requireNonNull(reasonType,"reasonType"); this.reasonDetail=requireDetail(reasonDetail); this.status=ReportStatus.PENDING;
 }
 public static Report forPetition(User u,Petition p,ReportReasonType t,String d){return new Report(u,p,null,ReportTargetType.PETITION,t,d);}
 public static Report forComment(User u,Comment c,ReportReasonType t,String d){return new Report(u,null,c,ReportTargetType.COMMENT,t,d);}
 public void process(ReportStatus s,ReportActionType actionType,Admin a,String reason,LocalDateTime at){if(s==ReportStatus.PENDING || status!=ReportStatus.PENDING) throw new IllegalStateException(); if(s==ReportStatus.ACTION_TAKEN && actionType==null) throw new IllegalStateException(); if(s==ReportStatus.DISMISSED && actionType!=null) throw new IllegalStateException(); status=s; this.actionType=actionType; processedByAdmin=Objects.requireNonNull(a); processingReason=require(reason,"processingReason",500); processedAt=Objects.requireNonNull(at);}
 private static String requireDetail(String v){if(v==null||v.isBlank()||v.length()<10||v.length()>500)throw new IllegalArgumentException("reasonDetail");return v;} private static String require(String v,String n,int max){if(v==null||v.isBlank()||v.length()>max)throw new IllegalArgumentException(n);return v;}
 public Long getId(){return id;} public User getReporter(){return reporter;} public Petition getPetition(){return petition;} public Comment getComment(){return comment;}
 public ReportTargetType getTargetType(){return targetType;} public ReportReasonType getReasonType(){return reasonType;} public String getReasonDetail(){return reasonDetail;}
 public ReportStatus getStatus(){return status;} public ReportActionType getActionType(){return actionType;} public Admin getProcessedByAdmin(){return processedByAdmin;} public LocalDateTime getProcessedAt(){return processedAt;} public String getProcessingReason(){return processingReason;}
}