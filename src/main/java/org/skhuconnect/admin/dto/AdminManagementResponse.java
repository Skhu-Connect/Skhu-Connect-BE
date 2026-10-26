package org.skhuconnect.admin.dto;

public record AdminManagementResponse(
        Long id,
        String loginId,
        String role
) {
}
