package org.skhuconnect.report.exception;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ReportExceptionHandler{
 @ExceptionHandler(ReportException.class) ProblemDetail handle(ReportException e){HttpStatus s=switch(e.getReason()){case INVALID_TARGET,INVALID_STATUS,INVALID_PAGE,MISSING_ACTION_TYPE->HttpStatus.BAD_REQUEST;case SELF_REPORT,ALREADY_REPORTED->HttpStatus.CONFLICT;default->HttpStatus.NOT_FOUND;}; ProblemDetail p=ProblemDetail.forStatus(s);p.setTitle(e.getReason().name());return p;}
}