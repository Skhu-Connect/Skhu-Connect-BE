package org.skhuconnect.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI skhuConnectOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SKHU Connect API")
                        .description("성공회대학교 학생 의견 연결 플랫폼 API")
                        .version("v1"));
    }
}
