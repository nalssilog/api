package com.nalssilog.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.security.ApiAuthenticationEntryPoint;
import com.nalssilog.auth.member.MemberClient;
import com.nalssilog.auth.web.AuthCookieManager;
import com.nalssilog.common.security.VerifiedRequestCredentials;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.MemberRole;
import com.nalssilog.member.domain.Provider;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

@SuppressWarnings("java:S5960")
class JwtAuthenticationFilterTest {

    private final AuthCookieManager cookieManager =
            mock(AuthCookieManager.class);
    private final RefreshTokenStore refreshTokenStore =
            mock(RefreshTokenStore.class);
    private final JwtTokenProvider tokenProvider =
            new JwtTokenProvider(TestAuthProperties.create());
    private final MemberClient memberClient = mock(MemberClient.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(
                    cookieManager,
                    tokenProvider,
                    refreshTokenStore,
                    new ApiAuthenticationEntryPoint(new ObjectMapper()),
                    memberClient);

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerIsAuthenticatedWithoutConsultingCookies() throws Exception {
        String accessToken = tokenProvider.createAccessToken(
                7L,
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                "session-7");
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/members/me");

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            invoked.set(true);

            var authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            assertThat(authentication.getPrincipal()).isEqualTo(7L);
            assertThat(authentication.getDetails())
                    .isEqualTo(new AuthRequestDetails(
                            Provider.KAKAO,
                            "session-7",
                            CredentialTransport.BEARER));
        });

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(VerifiedRequestCredentials.hasBearer(request)).isTrue();
        verifyNoInteractions(cookieManager);
    }

    @Test
    void malformedAuthorizationNeverFallsBackToCookieAuthentication()
            throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/members/me");

        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer");

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) -> invoked.set(true));

        assertThat(invoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
                .contains("\"code\":\"AUTH_ACCESS_TOKEN_INVALID\"");
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
        verifyNoInteractions(cookieManager);
    }

    @Test
    void adminRequestLoadsCurrentOperationalRoleFromMemberStore() throws Exception {
        when(memberClient.findRole(7L)).thenReturn(Optional.of(MemberRole.ADMIN));
        String accessToken = tokenProvider.createAccessToken(
                7L, MemberStatus.ACTIVE, Provider.KAKAO, "session-7");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/admin/report-flags");

        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        filter.doFilter(request, new MockHttpServletResponse(), (_, _) -> {
            var authorities = SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getAuthorities();

            assertThat(authorities)
                    .extracting(authority -> authority.getAuthority())
                    .contains("ROLE_MEMBER", "ROLE_MODERATOR", "ROLE_ADMIN");
        });
    }

    @Test
    void expiredBearerReturnsTheStableExpiredCode() throws Exception {
        JwtTokenProvider expiredTokenProvider =
                new JwtTokenProvider(TestAuthProperties.create(
                        Duration.ofSeconds(-1)));
        JwtAuthenticationFilter expiredFilter =
                new JwtAuthenticationFilter(
                        cookieManager,
                        expiredTokenProvider,
                        refreshTokenStore,
                        new ApiAuthenticationEntryPoint(new ObjectMapper()),
                        mock(MemberClient.class));
        String expiredToken = expiredTokenProvider.createAccessToken(
                7L,
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                "session-7");
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/members/me");

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + expiredToken);

        MockHttpServletResponse response = new MockHttpServletResponse();

        expiredFilter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) -> {
                    throw new AssertionError("expired bearer must stop the chain");
                });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
                .contains("\"code\":\"AUTH_ACCESS_TOKEN_EXPIRED\"");
    }
}
