package org.skhuconnect.admin.content.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record AdminPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <S, T> AdminPageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new AdminPageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast());
    }
}