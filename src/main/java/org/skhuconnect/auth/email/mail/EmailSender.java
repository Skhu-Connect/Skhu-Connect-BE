package org.skhuconnect.auth.email.mail;

public interface EmailSender {
    void sendVerificationCode(String recipient, String code);
}
