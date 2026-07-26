package com.nalssilog.common.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청마다 correlation-id(traceId)를 MDC 에 심어 로그 전 구간에서 추적 가능하게 한다.
 * 상류(게이트웨이·다른 서비스)에서 전달한 X-Trace-Id 가 있으면 이어받고, 없으면 새로 발급한다.
 * MSA 분리 시 서비스 간 요청에 이 헤더를 전파하면 분산 추적이 그대로 이어진다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

	public static final String TRACE_ID = "traceId";
	private static final String TRACE_ID_HEADER = "X-Trace-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String traceId = resolveTraceId(request);

		MDC.put(TRACE_ID, traceId);
		response.setHeader(TRACE_ID_HEADER, traceId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(TRACE_ID);
		}
	}

	private String resolveTraceId(HttpServletRequest request) {
		String inbound = request.getHeader(TRACE_ID_HEADER);

		if (StringUtils.hasText(inbound)) {
			return inbound;
		}

		return UUID.randomUUID().toString().substring(0, 8);
	}
}
