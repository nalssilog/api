package com.nalssilog.report.config;

import com.nalssilog.common.web.TrustedProxyChain;
import com.nalssilog.member.config.FeedbackRateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ReportClientIpResolver {

    private final TrustedProxyChain trustedProxyChain;

    public ReportClientIpResolver(
            ReportRateLimitProperties properties,
            FeedbackRateLimitProperties fallbackProperties
    ) {
        this.trustedProxyChain = new TrustedProxyChain(
                properties.trustedProxies().isEmpty()
                        ? fallbackProperties.trustedProxies()
                        : properties.trustedProxies());
    }

    public String resolve(HttpServletRequest request) {
        return trustedProxyChain.resolve(request);
    }
}
