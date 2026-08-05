package org.skhuconnect.petition.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PetitionPageResponse(
        List<PetitionQueryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static PetitionPageResponse from(Page<PetitionQueryResponse> page) {
        return new PetitionPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}