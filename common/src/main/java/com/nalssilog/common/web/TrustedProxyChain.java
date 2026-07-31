package com.nalssilog.common.web;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.util.StringUtils;

/**
 * 실제 socket peer가 명시된 trusted proxy 대역일 때만 X-Forwarded-For를 해석한다.
 * 체인을 오른쪽부터 따라가 첫 untrusted hop을 실제 클라이언트 주소로 선택한다.
 */
public final class TrustedProxyChain {

    private static final int MAX_WRAPPER_DEPTH = 16;

    private final List<IpAddressMatcher> trustedProxies;

    public TrustedProxyChain(List<String> trustedProxies) {
        List<String> ranges = trustedProxies == null ? List.of() : trustedProxies;

        this.trustedProxies = ranges.stream()
                .map(IpAddressMatcher::new)
                .toList();
    }

    public String resolve(HttpServletRequest request) {
        HttpServletRequest nativeRequest = unwrap(request);
        String peer = normalize(nativeRequest.getRemoteAddr());

        if (!isTrusted(peer)) {
            return peer;
        }

        String forwardedFor = nativeRequest.getHeader("X-Forwarded-For");

        if (!StringUtils.hasText(forwardedFor)) {
            return peer;
        }

        String current = peer;
        String[] hops = forwardedFor.split(",");

        for (int index = hops.length - 1; index >= 0 && isTrusted(current); index--) {
            String candidate = normalize(hops[index]);

            if (!StringUtils.hasText(candidate)) {
                break;
            }

            current = candidate;
        }

        return current;
    }

    private HttpServletRequest unwrap(HttpServletRequest request) {
        ServletRequest current = request;
        int depth = 0;

        while (current instanceof ServletRequestWrapper wrapper && depth++ < MAX_WRAPPER_DEPTH) {
            ServletRequest nested = wrapper.getRequest();

            if (nested == current) {
                break;
            }

            current = nested;
        }

        return current instanceof HttpServletRequest httpRequest ? httpRequest : request;
    }

    private boolean isTrusted(String address) {
        if (!StringUtils.hasText(address)) {
            return false;
        }

        for (IpAddressMatcher matcher : trustedProxies) {
            try {
                if (matcher.matches(address)) {
                    return true;
                }
            } catch (IllegalArgumentException _) {
                return false;
            }
        }

        return false;
    }

    private String normalize(String address) {
        if (!StringUtils.hasText(address)) {
            return "unknown";
        }

        String normalized = address.strip();

        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        if (normalized.startsWith("[")) {
            int closingBracket = normalized.indexOf(']');

            if (closingBracket > 1) {
                return normalized.substring(1, closingBracket);
            }
        }

        int colon = normalized.lastIndexOf(':');

        if (colon > 0 && normalized.indexOf(':') == colon && normalized.substring(0, colon).contains(".")) {
            return normalized.substring(0, colon);
        }

        return normalized;
    }
}
