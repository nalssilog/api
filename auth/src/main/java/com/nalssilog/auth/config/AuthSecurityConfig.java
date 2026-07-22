package com.nalssilog.auth.config;

import com.nalssilog.auth.application.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${nalssilog.frontend.base-url}")
    private String frontendBaseUrl;

    @Bean
    // S1130/S112: HttpSecurity.build() 가 checked Exception 을 던져 throws Exception 이 강제됨(프레임워크 API).
    // S3330: CSRF 더블서브밋 토큰은 프론트 JS 가 읽어 X-XSRF-TOKEN 헤더로 되돌려야 하므로 HttpOnly=false 가 의도된 설계.
    @SuppressWarnings({"java:S1130", "java:S112", "java:S3330"})
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 쿠키 기반 인증이라 CSRF 방어 필요. SPA 더블 서브밋: XSRF-TOKEN 쿠키(JS 읽기 가능) ↔ X-XSRF-TOKEN 헤더.
                // 프론트는 상태변경(POST/PUT/PATCH/DELETE) 요청에 이 헤더를 실어야 한다. (GET·OAuth 리다이렉트는 면제)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/api/health", "/oauth2/**", "/login/oauth2/**").permitAll()
                        // 소셜 로그인 진입·세션 조회·가입 확정·연동 흐름 (JWT 없이 티켓/쿠키로 식별)
                        .requestMatchers("/api/auth/login/**", "/api/auth/me", "/api/auth/signup").permitAll()
                        // 설정에서 시작하는 소셜 추가 연동은 로그인 상태 전용 (아래 link/** permitAll 보다 먼저 매칭)
                        .requestMatchers("/api/auth/link/social/**").authenticated()
                        .requestMatchers("/api/auth/refresh", "/api/auth/logout", "/api/auth/link/**").permitAll()
                        // 즐겨찾기는 회원 전용 (아래 지역 GET 허용보다 먼저 매칭돼야 함)
                        .requestMatchers("/api/locations/favorites", "/api/locations/favorites/**").authenticated()
                        // 내 제보 목록은 회원 전용 (아래 report GET 허용보다 먼저 매칭돼야 함)
                        .requestMatchers(HttpMethod.GET, "/api/reports/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/locations/**", "/api/reports/**").permitAll()
                        // 닉네임 중복확인은 가입(비로그인) 단계에서도 써야 하므로 공개
                        .requestMatchers(HttpMethod.GET, "/api/members/nickname/availability").permitAll()
                        // 본인 계정 조회는 인증 필요 (아래 공개 프로필 permitAll 보다 먼저 매칭)
                        .requestMatchers(HttpMethod.GET, "/api/members/me").authenticated()
                        // 공개 회원 프로필 조회 (단일 세그먼트만 — /me·/me/**·/nickname/** 는 인증 유지)
                        .requestMatchers(HttpMethod.GET, "/api/members/*").permitAll()
                        // 서비스 피드백은 비로그인도 제출 가능 (memberId 는 있으면 기록)
                        .requestMatchers(HttpMethod.POST, "/api/feedbacks").permitAll()
                        // 익명 제보·감사해요 허용 (익명 UUID 쿠키로 식별)
                        .requestMatchers(HttpMethod.POST, "/api/reports/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/reports/*/thanks").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_SESSION_EXPIRED", "로그인이 필요합니다."))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (accessDeniedException instanceof MissingCsrfTokenException) {
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "CSRF_TOKEN_MISSING", "CSRF 토큰이 없습니다.");
                            } else if (accessDeniedException instanceof InvalidCsrfTokenException) {
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "CSRF_TOKEN_INVALID", "CSRF 토큰이 유효하지 않습니다.");
                            } else {
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다.");
                            }
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendBaseUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
