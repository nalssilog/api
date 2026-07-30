package com.nalssilog.auth.web;

import com.nalssilog.auth.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieManager {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    public static final String SIGNUP_TICKET_COOKIE = "signup_ticket";
    public static final String LINK_TICKET_COOKIE = "link_ticket";
    public static final String LINK_INTENT_COOKIE = "link_intent";

    private final AuthProperties properties;

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        addCookie(response, ACCESS_TOKEN_COOKIE, accessToken, properties.jwt().accessTokenTtl());
    }

    public void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        addAuthCookies(response, accessToken, refreshToken, properties.jwt().refreshTokenTtl());
    }

    public void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken,
                               Duration refreshTokenMaxAge) {
        addAccessTokenCookie(response, accessToken);

        Duration maxAge = refreshTokenMaxAge == null
                ? properties.jwt().refreshTokenTtl()
                : refreshTokenMaxAge;

        addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, maxAge);
    }

    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", Duration.ZERO);
        addCookie(response, REFRESH_TOKEN_COOKIE, "", Duration.ZERO);
    }

    public Optional<String> readAccessToken(HttpServletRequest request) {

        return readCookie(request, ACCESS_TOKEN_COOKIE);
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {

        return readCookie(request, REFRESH_TOKEN_COOKIE);
    }

    public void addSignupTicketCookie(HttpServletResponse response, String ticketId) {
        addCookie(response, SIGNUP_TICKET_COOKIE, ticketId, properties.ticket().ttl());
    }

    public void clearSignupTicketCookie(HttpServletResponse response) {
        addCookie(response, SIGNUP_TICKET_COOKIE, "", Duration.ZERO);
    }

    public Optional<String> readSignupTicket(HttpServletRequest request) {

        return readCookie(request, SIGNUP_TICKET_COOKIE);
    }

    public void addLinkTicketCookie(HttpServletResponse response, String ticketId) {
        addCookie(response, LINK_TICKET_COOKIE, ticketId, properties.ticket().ttl());
    }

    public void clearLinkTicketCookie(HttpServletResponse response) {
        addCookie(response, LINK_TICKET_COOKIE, "", Duration.ZERO);
    }

    public Optional<String> readLinkTicket(HttpServletRequest request) {

        return readCookie(request, LINK_TICKET_COOKIE);
    }

    public void addLinkIntentCookie(HttpServletResponse response, String intentId) {
        addCookie(response, LINK_INTENT_COOKIE, intentId, properties.ticket().ttl());
    }

    public void clearLinkIntentCookie(HttpServletResponse response) {
        addCookie(response, LINK_INTENT_COOKIE, "", Duration.ZERO);
    }

    public Optional<String> readLinkIntent(HttpServletRequest request) {

        return readCookie(request, LINK_INTENT_COOKIE);
    }

    // 인증 쿠키는 host-only(Domain 생략) — dev/prod 인증쿠키 상호 덮어쓰기 방지, localhost 도 동작.
    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.cookie().secure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {

            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }
}
