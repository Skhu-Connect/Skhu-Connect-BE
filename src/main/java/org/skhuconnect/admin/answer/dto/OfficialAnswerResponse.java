package org.skhuconnect.admin.answer.dto;

import org.skhuconnect.admin.answer.entity.AnswerSource;
import org.skhuconnect.admin.answer.entity.OfficialAnswer;

import java.time.LocalDateTime;

public record OfficialAnswerResponse(
        Long id,
        Long petitionId,
        Long adminId,
        String content,
        AnswerSource answerSource,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OfficialAnswerResponse from(OfficialAnswer answer) {
        return new OfficialAnswerResponse(
                answer.getId(),
                answer.getPetition().getId(),
                answer.getAdmin().getId(),
                answer.getContent(),
                answer.getAnswerSource(),
                answer.getCreatedAt(),
                answer.getUpdatedAt()
        );
    }
}