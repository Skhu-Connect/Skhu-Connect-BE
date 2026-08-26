package org.skhuconnect.auth.loginid.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 아이디 응답")
public record LoginIdResponse(
        @Schema(description = "전체 로그인 아이디", example = "202214139")
        String loginId
) {
}
