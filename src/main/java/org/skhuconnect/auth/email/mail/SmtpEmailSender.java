package org.skhuconnect.auth.email.mail;

import org.skhuconnect.global.config.AppMailProperties;
import org.skhuconnect.auth.email.exception.EmailDeliveryException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender mailSender;
    private final AppMailProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, AppMailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendVerificationCode(String recipient, String code) {
        if (properties.getFrom() == null || properties.getFrom().isBlank()) {
            throw new EmailDeliveryException(new IllegalStateException());
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(recipient);
        message.setSubject("[SKHU Connect] 이메일 인증번호 안내");
        message.setText("SKHU Connect 이메일 인증 안내\n인증번호: " + code
                + "\n유효시간: 5분\n본인이 요청하지 않았다면 이 메일을 무시해 주세요.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new EmailDeliveryException(exception);
        }
    }
}
