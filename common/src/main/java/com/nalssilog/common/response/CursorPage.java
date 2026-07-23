package com.nalssilog.common.response;

import java.util.List;

/**
 * 커서 기반 무한 스크롤 공통 응답. nextCursor 가 null 이면 마지막 페이지.
 */
public record CursorPage<T>(List<T> items, String nextCursor) {

    public static <T> CursorPage<T> of(List<T> items, String nextCursor) {
        return new CursorPage<>(items, nextCursor);
    }

    public static <T> CursorPage<T> last(List<T> items) {
        return new CursorPage<>(items, null);
    }
}
