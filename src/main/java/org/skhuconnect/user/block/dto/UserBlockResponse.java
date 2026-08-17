package org.skhuconnect.user.block.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "영구 차단 처리 결과")
public record UserBlockResponse(
        @Schema(description = "차단 생성 시각") LocalDateTime createdAt
) { }
