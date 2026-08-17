package org.skhuconnect.user.block.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.skhuconnect.user.block.entity.BlockTargetType;

@Schema(description = "영구 차단할 콘텐츠 정보")
public record UserBlockRequest(
        @NotNull @Schema(description = "차단 대상 콘텐츠 종류. PETITION: 청원 작성자, COMMENT: 댓글 또는 대댓글 작성자", example = "COMMENT") BlockTargetType targetType,
        @NotNull @Schema(description = "청원 또는 댓글·대댓글 ID", example = "42") Long contentId
) { }
