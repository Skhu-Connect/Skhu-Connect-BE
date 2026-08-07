package org.skhuconnect.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
public record FcmTokenRequest(@NotBlank String token) {}
