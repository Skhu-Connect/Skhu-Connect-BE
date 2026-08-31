package org.skhuconnect.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    private static final String VERCEL_FRONTEND_ORIGIN = "https://petition-system-two.vercel.app";
    private static final String SERVICE_FRONTEND_ORIGIN = "https://seokhwan.store"; // www 는 apex 로 307 리다이렉트되므로 apex 만 허용한다
    private static final String LOCAL_FRONTEND_ORIGIN = "http://localhost:5173";

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(VERCEL_FRONTEND_ORIGIN, SERVICE_FRONTEND_ORIGIN, LOCAL_FRONTEND_ORIGIN)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}