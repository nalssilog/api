package com.nalssilog.common.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청마다 액세스 로그 한 줄(method·path·status·소요시간·actor). {@link TraceIdFilter} 다음 순서라 traceId 가 이미 MDC 에 있다.
 * prod JSON 용으로 필드를 MDC 에 잠시 싣는다. 작성자: 인증 필터가 심은 request attribute({@link #ACTOR_MEMBER_ID}), 없으면 쿠키.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

	/** 인증 필터가 인증된 회원의 memberId 를 실어두는 request attribute 키. */
	public static final String ACTOR_MEMBER_ID = "actorMemberId";

	/** report 모듈의 익명 식별 쿠키명과의 계약(모듈 역의존 회피용 상수 복제). */
	private static final String ANONYMOUS_COOKIE = "anonymous_id";

	private static final Logger log = LoggerFactory.getLogger("http.access");

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		long startNanos = System.nanoTime();

		try {
			filterChain.doFilter(request, response);
		} finally {
			long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
			String actor = resolveActor(request);
			String query = request.getQueryString();

			MDC.put("method", request.getMethod());
			MDC.put("path", request.getRequestURI());
			MDC.put("status", String.valueOf(response.getStatus()));
			MDC.put("durationMs", String.valueOf(durationMs));
			MDC.put("actor", actor);

			try {
				log.info("{} {}{} -> {} ({}ms) actor={}",
					request.getMethod(),
					request.getRequestURI(),
					query == null ? "" : "?" + query,
					response.getStatus(),
					durationMs,
					actor);
			} finally {
				MDC.remove("method");
				MDC.remove("path");
				MDC.remove("status");
				MDC.remove("durationMs");
				MDC.remove("actor");
			}
		}
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();

		return uri.equals("/api/health") || uri.startsWith("/error");
	}

	private String resolveActor(HttpServletRequest request) {
		Object memberId = request.getAttribute(ACTOR_MEMBER_ID);

		if (memberId != null) {
			return "member:" + memberId;
		}

		Cookie[] cookies = request.getCookies();

		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (ANONYMOUS_COOKIE.equals(cookie.getName())) {
					String value = cookie.getValue();

					return "anon:" + (value.length() > 8 ? value.substring(0, 8) : value);
				}
			}
		}

		return "guest";
	}
}
