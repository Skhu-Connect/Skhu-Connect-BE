package org.skhuconnect.auth.email.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skhuconnect.auth.email.exception.EmailDeliveryException;
import org.skhuconnect.global.config.AppMailProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendEmailSenderTest {
    private MockRestServiceServer server;
    private ResendEmailSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        AppMailProperties properties = new AppMailProperties();
        properties.setFrom("SKHU Connect <noreply@example.com>");
        properties.setResendApiKey("re_test_key");
        sender = new ResendEmailSender(builder, properties);
    }

    @Test
    void sendsVerificationCodeThroughResendHttpApi() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer re_test_key"))
                .andExpect(header("User-Agent", "skhu-connect/1.0"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "from": "SKHU Connect <noreply@example.com>",
                          "to": ["student@office.skhu.ac.kr"],
                          "subject": "[SKHU Connect] 이메일 인증번호 안내",
                          "text": "SKHU Connect 이메일 인증 안내\n인증번호: 012345\n유효시간: 5분\n본인이 요청하지 않았다면 이 메일을 무시해 주세요."
                        }
                        """))
                .andRespond(withSuccess("{\"id\":\"email-id\"}", MediaType.APPLICATION_JSON));

        sender.sendVerificationCode("student@office.skhu.ac.kr", "012345");

        server.verify();
    }

    @Test
    void convertsResendFailureToExistingDeliveryException() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> sender.sendVerificationCode(
                "student@office.skhu.ac.kr", "012345"))
                .isInstanceOf(EmailDeliveryException.class);
    }
}
