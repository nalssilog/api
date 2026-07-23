package com.nalssilog.report.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 익명 사용자 식별용 HttpOnly UUID 쿠키. 제보 작성·감사해요에서 동일 쿠키로 같은 익명 사용자를 식별한다.
 */
@Component
public class AnonymousIdManager {

    public static final String COOKIE = "anonymous_id";
    private static final Duration TTL = Duration.ofDays(365);

    @Value("${nalssilog.auth.cookie.secure:false}")
    private boolean secure;

    public Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    public String getOrIssue(HttpServletRequest request, HttpServletResponse response) {
        return read(request).orElseGet(() -> issue(response));
    }

    private String issue(HttpServletResponse response) {
        String anonymousId = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(COOKIE, anonymousId)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(TTL)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return anonymousId;
    }
}
