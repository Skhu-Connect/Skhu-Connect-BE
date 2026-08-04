package org.skhuconnect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "app.mail.from=test@example.com",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.jwt.cookie-secure=false"
})
class SkhuConnectApplicationTests {

    @Test
    void contextLoads() {
    }

}
