package com.nalssilog.auth.mobile.guest;

import com.nalssilog.auth.core.AuthErrorCode;
import com.nalssilog.auth.security.ApiAuthenticationEntryPoint;
import com.nalssilog.auth.security.CredentialAuthenticationException;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.common.security.VerifiedRequestCredentials;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class MobileGuestCredentialFilter extends OncePerRequestFilter {

    private final MobileGuestCredentialService credentialService;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        List<String> values = Collections.list(
                request.getHeaders(MobileGuestCredentialService.HEADER));

        if (values.isEmpty()) {
            filterChain.doFilter(request, response);

            return;
        }

        if (values.size() != 1 || values.getFirst() == null
                || values.getFirst().isBlank() || values.getFirst().contains(",")) {
            reject(request, response, AuthErrorCode.GUEST_CREDENTIAL_INVALID);

            return;
        }

        try {
            String anonymousKey = credentialService.authenticate(values.getFirst());

            VerifiedRequestCredentials.markGuest(request, anonymousKey);
            filterChain.doFilter(request, response);
        } catch (NalssiLogException exception) {
            AuthErrorCode errorCode = exception.getErrorCode() instanceof AuthErrorCode authErrorCode
                    ? authErrorCode
                    : AuthErrorCode.GUEST_CREDENTIAL_INVALID;

            reject(request, response, errorCode);
        }
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
                new CredentialAuthenticationException(errorCode, false));
    }
}
