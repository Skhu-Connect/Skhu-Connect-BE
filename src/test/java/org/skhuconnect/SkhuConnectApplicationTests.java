package org.skhuconnect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.mail.from=test@example.com",
        "app.mail.resend-api-key=re_test_key",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
class SkhuConnectApplicationTests {

    @Test
    void contextLoads() {
    }

}
