package com.nalssilog.auth.token;

import com.nalssilog.auth.core.AuthErrorCode;
import com.nalssilog.auth.mobile.guest.MobileGuestCredentialService;
import com.nalssilog.auth.oauth.SocialAuthPrincipal;
import com.nalssilog.auth.security.ApiAuthenticationEntryPoint;
import com.nalssilog.auth.security.CredentialAuthenticationException;
import com.nalssilog.auth.token.JwtTokenProvider.AccessTokenPayload;
import com.nalssilog.auth.token.JwtTokenProvider.TokenValidation;
import com.nalssilog.auth.token.JwtTokenProvider.TokenValidationStatus;
import com.nalssilog.auth.web.AuthCookieManager;
import com.nalssilog.common.filter.RequestLoggingFilter;
import com.nalssilog.common.security.VerifiedRequestCredentials;
import com.nalssilog.member.domain.MemberStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "^Bearer ([A-Za-z0-9\\-._~+/]+=*)$",
            Pattern.CASE_INSENSITIVE);

    private final AuthCookieManager cookieManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        List<String> authorizationHeaders =
                Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));

        if (!authorizationHeaders.isEmpty()) {
            authenticateBearer(request, response, authorizationHeaders);
            if (response.isCommitted()) {

                return;
            }
        } else if (allowsCookieFallback(request)) {
            authenticateCookie(request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateBearer(
            HttpServletRequest request,
            HttpServletResponse response,
            List<String> values
    ) throws IOException {
        if (values.size() != 1 || values.getFirst() == null || values.getFirst().contains(",")) {
            reject(request, response, AuthErrorCode.AUTH_ACCESS_TOKEN_INVALID);

            return;
        }

        Matcher matcher = BEARER_PATTERN.matcher(values.getFirst());

        if (!matcher.matches()) {
            reject(request, response, AuthErrorCode.AUTH_ACCESS_TOKEN_INVALID);

            return;
        }

        TokenValidation validation = jwtTokenProvider.validate(matcher.group(1));

        if (validation.status() == TokenValidationStatus.EXPIRED) {
            reject(request, response, AuthErrorCode.AUTH_ACCESS_TOKEN_EXPIRED);

            return;
        }
        if (validation.status() != TokenValidationStatus.VALID
                || !isUsable(validation.payload())) {
            reject(request, response, AuthErrorCode.AUTH_ACCESS_TOKEN_INVALID);

            return;
        }
        if (isRevoked(validation.payload())) {
            reject(request, response, AuthErrorCode.AUTH_SESSION_EXPIRED);

            return;
        }

        setAuthentication(request, validation.payload(), CredentialTransport.BEARER);
        VerifiedRequestCredentials.markBearer(request);
    }

    private void authenticateCookie(HttpServletRequest request) {
        cookieManager.readAccessToken(request)
                .flatMap(jwtTokenProvider::parse)
                .filter(this::isUsable)
                .filter(payload -> !isRevoked(payload))
                .ifPresent(payload -> setAuthentication(request, payload, CredentialTransport.COOKIE));
    }

    private boolean isUsable(AccessTokenPayload payload) {

        return payload != null && payload.status() != MemberStatus.WITHDRAWN;
    }

    private boolean isRevoked(AccessTokenPayload payload) {

        return payload.sessionId() != null
                && !payload.sessionId().isBlank()
                && refreshTokenStore.isSessionRevoked(payload.sessionId());
    }

    private void setAuthentication(
            HttpServletRequest request,
            AccessTokenPayload payload,
            CredentialTransport transport
    ) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                payload.memberId(),
                null,
                List.of(new SimpleGrantedAuthority(SocialAuthPrincipal.roleOf(payload.status()))));

        authentication.setDetails(new AuthRequestDetails(
                payload.provider(), payload.sessionId(), transport));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(RequestLoggingFilter.ACTOR_MEMBER_ID, payload.memberId());
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthErrorCode errorCode
    ) throws IOException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(
                request,
                response,
                new CredentialAuthenticationException(errorCode));
    }

    private boolean allowsCookieFallback(HttpServletRequest request) {
        String path = request.getRequestURI();

        return !path.startsWith("/api/auth/mobile/")
                && !path.equals("/api/mobile/guests")
                && request.getHeader(MobileGuestCredentialService.HEADER) == null;
    }
}
