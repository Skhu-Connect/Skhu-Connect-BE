package org.skhuconnect.user.block.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "차단 대상 콘텐츠 종류. PETITION은 청원 작성자, COMMENT는 댓글 또는 대댓글 작성자를 뜻합니다.")
public enum BlockTargetType {
    @Schema(description = "청원 ID로 해당 청원 작성자를 차단합니다.")
    PETITION,
    @Schema(description = "댓글 또는 대댓글 ID로 해당 작성자를 차단합니다.")
    COMMENT
}
