package org.skhuconnect.report.exception;
public class ReportException extends RuntimeException{
 public enum Reason{INVALID_TARGET, PETITION_NOT_FOUND, COMMENT_NOT_FOUND, SELF_REPORT, ALREADY_REPORTED, REPORT_NOT_FOUND, INVALID_STATUS, INVALID_PAGE}
 private final Reason reason; public ReportException(Reason r){super(r.name());reason=r;} public Reason getReason(){return reason;}
}