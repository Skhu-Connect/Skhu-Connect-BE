package org.skhuconnect.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void openApiContainsSkhuConnectInformation() {
        OpenAPI openAPI = new OpenApiConfig().skhuConnectOpenAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("SKHU Connect API");
        assertThat(openAPI.getInfo().getDescription())
                .isEqualTo("성공회대학교 학생 의견 연결 플랫폼 API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
    }

    @Test
    void openApiContainsJwtBearerAuthentication() {
        OpenAPI openAPI = new OpenApiConfig().skhuConnectOpenAPI();

        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes()
                .get(OpenApiConfig.BEARER_AUTH);
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
        assertThat(openAPI.getSecurity()).hasSize(1);
        assertThat(openAPI.getSecurity().get(0))
                .containsKey(OpenApiConfig.BEARER_AUTH);
    }
}
