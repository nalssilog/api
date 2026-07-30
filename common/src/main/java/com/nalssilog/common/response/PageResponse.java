package com.nalssilog.common.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 전체 개수와 페이지 수를 포함하는 번호 기반 페이지 응답.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {

    public PageResponse {
        items = List.copyOf(items);
    }

    public static <T> PageResponse<T> from(Page<T> source) {

        return new PageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.hasPrevious(),
                source.hasNext());
    }

    public static <T> PageResponse<T> of(
            List<T> items,
            int page,
            int size,
            long totalElements
    ) {
        if (page < 0 || size < 1 || totalElements < 0) {
            throw new IllegalArgumentException("invalid page metadata");
        }

        int totalPages = Math.toIntExact(Math.ceilDiv(totalElements, size));

        return new PageResponse<>(
                items,
                page,
                size,
                totalElements,
                totalPages,
                page > 0 && totalPages > 0,
                page + 1 < totalPages);
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {

        return new PageResponse<>(
                items.stream().map(mapper).toList(),
                page,
                size,
                totalElements,
                totalPages,
                hasPrevious,
                hasNext);
    }
}
