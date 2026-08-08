package org.skhuconnect.petition.dto.response;

import org.skhuconnect.admin.answer.entity.AnswerSource;
import org.skhuconnect.admin.answer.entity.OfficialAnswer;

import java.time.LocalDateTime;

public record OfficialAnswerDetailResponse(
        String content,
        AnswerSource answerSource,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OfficialAnswerDetailResponse from(OfficialAnswer answer) {
        return new OfficialAnswerDetailResponse(
                answer.getContent(),
                answer.getAnswerSource(),
                answer.getCreatedAt(),
                answer.getUpdatedAt()
        );
    }
}