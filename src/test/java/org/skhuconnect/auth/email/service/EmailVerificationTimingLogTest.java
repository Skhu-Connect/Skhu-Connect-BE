package org.skhuconnect.auth.email.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.email.entity.EmailVerificationPurpose;
import org.skhuconnect.auth.email.exception.EmailVerificationException;
import org.skhuconnect.auth.email.mail.EmailSender;
import org.skhuconnect.auth.email.repository.EmailVerificationRepository;
import org.skhuconnect.user.repository.UserRepository;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailVerificationTimingLogTest {
    private UserRepository userRepository;
    private EmailVerificationRepository repository;
    private VerificationCodeGenerator codeGenerator;
    private EmailVerificationService service;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        repository = mock(EmailVerificationRepository.class);
        userRepository = mock(UserRepository.class);
        codeGenerator = mock(VerificationCodeGenerator.class);
        service = new EmailVerificationService(repository, userRepository,
                new EmailNormalizer(), codeGenerator,
                mock(VerificationTokenGenerator.class), new VerificationHasher(),
                mock(EmailSender.class),
                Clock.fixed(Instant.parse("2026-08-04T03:00:00Z"), ZoneOffset.UTC));

        logger = (Logger) LoggerFactory.getLogger(EmailVerificationService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void successLogContainsOnlyPurposeResultAndDurations() {
        String sensitiveEmail = "private-student@office.skhu.ac.kr";
        String sensitiveCode = "987654";
        when(userRepository.existsByEmail(sensitiveEmail)).thenReturn(false);
        when(repository.findByEmailAndPurpose(
                sensitiveEmail, EmailVerificationPurpose.SIGN_UP))
                .thenReturn(Optional.empty());
        when(codeGenerator.generate()).thenReturn(sensitiveCode);

        service.sendCode(sensitiveEmail, EmailVerificationPurpose.SIGN_UP);

        assertThat(singleLog())
                .contains("purpose=SIGN_UP", "success=true",
                        "user_exists_ms=", "verification_lookup_ms=",
                        "save_flush_ms=", "mail_send_ms=", "total_ms=")
                .doesNotContain(sensitiveEmail, sensitiveCode,
                        "verificationToken", "MAIL_USERNAME",
                        "SKHU Connect 이메일 인증 안내");
    }

    @Test
    void earlyFailureLogMarksUnexecutedSegmentsWithoutEmail() {
        String sensitiveEmail = "student@office.skhu.ac.kr";
        when(userRepository.existsByEmail(sensitiveEmail)).thenReturn(true);

        assertThatThrownBy(() -> service.sendCode(
                sensitiveEmail, EmailVerificationPurpose.SIGN_UP))
                .isInstanceOf(EmailVerificationException.class);

        assertThat(singleLog())
                .contains("purpose=SIGN_UP", "success=false",
                        "verification_lookup_ms=-1", "save_flush_ms=-1",
                        "mail_send_ms=-1", "total_ms=")
                .doesNotContain(sensitiveEmail);
    }

    private String singleLog() {
        assertThat(appender.list).hasSize(1);
        return appender.list.get(0).getFormattedMessage();
    }
}
