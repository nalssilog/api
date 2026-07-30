package com.nalssilog.auth.oauth.apple;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleAuthorizationResponseFilter extends OncePerRequestFilter {

    public static final String CALLBACK_PATH = "/login/oauth2/code/apple";
    private static final int MAX_USER_JSON_LENGTH = 4_096;
    private static final int MAX_SOCIAL_NAME_LENGTH = 100;

    private final ObjectMapper objectMapper;
    private final AppleAuthorizationUserContext userContext;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        return !HttpMethod.POST.matches(request.getMethod())
                || !CALLBACK_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            userContext.set(readSocialName(request.getParameter("user")));
            filterChain.doFilter(request, response);
        } finally {
            userContext.clear();
        }
    }

    private String readSocialName(String payload) {
        if (payload == null || payload.isBlank()) {

            return null;
        }
        if (payload.length() > MAX_USER_JSON_LENGTH) {
            log.warn("auth.apple.user_payload_rejected reason=too_large");

            return null;
        }

        try {
            AppleUser user = objectMapper.readValue(payload, AppleUser.class);

            if (user == null || user.name() == null) {

                return null;
            }

            String firstName = sanitize(user.name().firstName());
            String lastName = sanitize(user.name().lastName());
            String combined = (firstName + " " + lastName).strip();

            if (combined.isBlank()) {

                return null;
            }

            return combined.length() <= MAX_SOCIAL_NAME_LENGTH
                    ? combined
                    : combined.substring(0, MAX_SOCIAL_NAME_LENGTH);
        } catch (JacksonException exception) {
            log.warn("auth.apple.user_payload_rejected reason=invalid_json");

            return null;
        }
    }

    private String sanitize(String value) {
        if (value == null) {

            return "";
        }

        return value.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .collect(
                        StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString()
                .strip();
    }

    private record AppleUser(AppleName name) {
    }

    private record AppleName(String firstName, String lastName) {
    }
}
