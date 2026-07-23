package com.nalssilog.auth.config;

import com.nalssilog.auth.application.JwtTokenProvider;
import com.nalssilog.auth.application.SocialAuthPrincipal;
import com.nalssilog.common.web.RequestLoggingFilter;
import com.nalssilog.member.domain.MemberStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthCookieManager cookieManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        cookieManager.readAccessToken(request)
                .flatMap(jwtTokenProvider::parse)
                .filter(payload -> payload.status() != MemberStatus.WITHDRAWN)
                .ifPresent(payload -> {
                    var authentication = UsernamePasswordAuthenticationToken.authenticated(
                            payload.memberId(),
                            null,
                            List.of(new SimpleGrantedAuthority(SocialAuthPrincipal.roleOf(payload.status()))));
                    authentication.setDetails(payload.provider());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    // STATELESS 라 체인 종료 후 SecurityContext 가 비므로, 액세스 로그용으로 memberId 를 남겨둔다.
                    request.setAttribute(RequestLoggingFilter.ACTOR_MEMBER_ID, payload.memberId());
                });
        filterChain.doFilter(request, response);
    }
}
