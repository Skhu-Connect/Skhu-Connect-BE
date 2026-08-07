package org.skhuconnect.auth.email.mail;

import org.skhuconnect.auth.email.exception.EmailDeliveryException;
import org.skhuconnect.global.config.AppMailProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class ResendEmailSender implements EmailSender {
    private static final String RESEND_EMAILS_URL = "https://api.resend.com/emails";
    private static final String SUBJECT = "[SKHU Connect] 이메일 인증번호 안내";

    private final RestClient client;
    private final AppMailProperties properties;

    public ResendEmailSender(RestClient.Builder builder, AppMailProperties properties) {
        this.client = builder.baseUrl(RESEND_EMAILS_URL).build();
        this.properties = properties;
    }

    @Override
    public void sendVerificationCode(String recipient, String code) {
        validateConfiguration();
        try {
            client.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getResendApiKey())
                    .header("User-Agent", "skhu-connect/1.0")
                    .body(Map.of(
                            "from", properties.getFrom(),
                            "to", new String[]{recipient},
                            "subject", SUBJECT,
                            "text", message(code)
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new EmailDeliveryException(exception);
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.getFrom()) || isBlank(properties.getResendApiKey())) {
            throw new EmailDeliveryException(
                    new IllegalStateException("Missing Resend mail configuration"));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String message(String code) {
        return "SKHU Connect 이메일 인증 안내\n인증번호: " + code
                + "\n유효시간: 5분\n본인이 요청하지 않았다면 이 메일을 무시해 주세요.";
    }
}
