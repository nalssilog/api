package com.nalssilog.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * 서명·저장소 검증이 끝난 비쿠키 credential만 요청 내부에 전달하는 계약.
 * 같은 이름의 외부 헤더는 읽지 않고 서버 필터만 request attribute를 설정한다.
 */
public final class VerifiedRequestCredentials {

    private static final String BEARER_ATTRIBUTE =
            VerifiedRequestCredentials.class.getName() + ".bearer";
    private static final String GUEST_ATTRIBUTE =
            VerifiedRequestCredentials.class.getName() + ".guestAnonymousKey";

    private VerifiedRequestCredentials() {
    }

    public static void markBearer(HttpServletRequest request) {
        request.setAttribute(BEARER_ATTRIBUTE, Boolean.TRUE);
    }

    public static boolean hasBearer(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(BEARER_ATTRIBUTE));
    }

    public static void markGuest(HttpServletRequest request, String anonymousKey) {
        request.setAttribute(GUEST_ATTRIBUTE, anonymousKey);
    }

    public static Optional<String> guestAnonymousKey(HttpServletRequest request) {
        Object value = request.getAttribute(GUEST_ATTRIBUTE);

        return value instanceof String text && !text.isBlank()
                ? Optional.of(text)
                : Optional.empty();
    }

    public static boolean hasGuest(HttpServletRequest request) {
        return guestAnonymousKey(request).isPresent();
    }

    public static boolean hasNonCookieCredential(HttpServletRequest request) {
        return hasBearer(request) || hasGuest(request);
    }
}
