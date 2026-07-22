package com.nalssilog.report.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.domain.ReportErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * createdAt + id 복합 커서를 Base64 로 인코딩. createdAt 은 ISO 문자열로 넣어 정밀도 손실 없이 비교한다.
 */
public final class CursorCodec {

    private CursorCodec() {
    }

    public record Cursor(Instant createdAt, Long id) {
    }

    public static String encode(Instant createdAt, Long id) {
        String raw = createdAt.toString() + "|" + id;

        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separator = raw.lastIndexOf('|');

            return new Cursor(Instant.parse(raw.substring(0, separator)), Long.parseLong(raw.substring(separator + 1)));
        } catch (RuntimeException _) {
            throw new NalssiLogException(ReportErrorCode.INVALID_CURSOR);
        }
    }
}
