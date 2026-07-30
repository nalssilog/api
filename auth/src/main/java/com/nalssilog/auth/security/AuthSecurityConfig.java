package com.nalssilog.auth.security;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.config.CorsProperties;
import com.nalssilog.auth.mobile.guest.MobileGuestCredentialFilter;
import com.nalssilog.auth.mobile.oauth.MobileOAuthAuthorizationRequestRepository;
import com.nalssilog.auth.mobile.oauth.MobileOAuthAuthorizationRequestResolver;
import com.nalssilog.auth.oauth.CustomOAuth2UserService;
import com.nalssilog.auth.oauth.CustomOidcUserService;
import com.nalssilog.auth.oauth.OAuth2LoginFailureHandler;
import com.nalssilog.auth.oauth.OAuth2LoginSuccessHandler;
import com.nalssilog.auth.oauth.apple.AppleAuthorizationResponseFilter;
import com.nalssilog.auth.token.JwtAuthenticationFilter;
import com.nalssilog.common.security.VerifiedRequestCredentials;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthSecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MobileGuestCredentialFilter mobileGuestCredentialFilter;
    private final AppleAuthorizationResponseFilter appleAuthorizationResponseFilter;
    private final MobileOAuthAuthorizationRequestResolver mobileOAuthAuthorizationRequestResolver;
    private final MobileOAuthAuthorizationRequestRepository mobileOAuthAuthorizationRequestRepository;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
            authorizationCodeTokenResponseClient;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final AuthProperties authProperties;
    private final CorsProperties corsProperties;

    @Bean
    @SuppressWarnings({"java:S1130", "java:S112"})
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(
                                this::isMobileCredentialEndpoint,
                                this::isAppleAuthorizationCallback,
                                VerifiedRequestCredentials::hasNonCookieCredential))
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/api/health", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/auth/login/**", "/api/auth/me", "/api/auth/signup").permitAll()
                        .requestMatchers(
                                "/api/auth/mobile/login/**",
                                "/api/auth/mobile/token",
                                "/api/auth/mobile/signup",
                                "/api/auth/mobile/refresh",
                                "/api/auth/mobile/logout",
                                "/api/auth/mobile/link/consent",
                                "/api/auth/mobile/link/cancel",
                                "/api/mobile/guests").permitAll()
                        .requestMatchers("/api/auth/mobile/link/social/**").authenticated()
                        .requestMatchers("/api/auth/link/social/**").authenticated()
                        .requestMatchers("/api/auth/refresh", "/api/auth/logout", "/api/auth/link/**").permitAll()
                        .requestMatchers("/api/locations/favorites", "/api/locations/favorites/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/reports/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/locations/**", "/api/reports/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/members/nickname/availability").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/members/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/members/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/feedbacks").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reports/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/reports/*").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/reports/*/thanks").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(mobileOAuthAuthorizationRequestResolver)
                                .authorizationRequestRepository(mobileOAuthAuthorizationRequestRepository))
                        .tokenEndpoint(endpoint -> endpoint
                                .accessTokenResponseClient(authorizationCodeTokenResponseClient))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (accessDeniedException instanceof MissingCsrfTokenException) {
                                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                        "CSRF_TOKEN_MISSING", "CSRF 토큰이 없습니다.");
                            } else if (accessDeniedException instanceof InvalidCsrfTokenException) {
                                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                        "CSRF_TOKEN_INVALID", "CSRF 토큰이 유효하지 않습니다.");
                            } else {
                                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                        "ACCESS_DENIED", "접근 권한이 없습니다.");
                            }
                        }))
                .addFilterBefore(jwtAuthenticationFilter, CsrfFilter.class)
                .addFilterAfter(mobileGuestCredentialFilter, JwtAuthenticationFilter.class)
                .addFilterBefore(
                        appleAuthorizationResponseFilter,
                        OAuth2LoginAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @SuppressWarnings("java:S3330")
    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();

        repository.setCookieName(authProperties.csrf().cookieName());
        repository.setCookieCustomizer(cookie -> {
            cookie.httpOnly(false)
                    .secure(authProperties.cookie().secure())
                    .sameSite("Lax")
                    .path("/");

            String domain = authProperties.csrf().cookieDomain();

            if (domain != null && !domain.isBlank()) {
                cookie.domain(domain);
            }
        });

        return repository;
    }

    private boolean isMobileCredentialEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/api/auth/mobile/")
                || path.equals("/api/mobile/guests");
    }

    private boolean isAppleAuthorizationCallback(HttpServletRequest request) {

        return HttpMethod.POST.matches(request.getMethod())
                && AppleAuthorizationResponseFilter.CALLBACK_PATH.equals(
                        request.getRequestURI());
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
