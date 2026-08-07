package org.skhuconnect.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void permitsOnlyConfiguredVercelOriginWithCredentials() {
        TestCorsRegistry registry = new TestCorsRegistry();
        new CorsConfig().corsConfigurer().addCorsMappings(registry);

        CorsConfiguration configuration = registry.configurations().get("/**");
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("https://petition-system-two.vercel.app");
        assertThat(configuration.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    private static class TestCorsRegistry extends CorsRegistry {
        private Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }
}