package com.nalssilog.member.config;

import com.nalssilog.common.web.TrustedProxyChain;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 실제 socket peer가 명시된 trusted proxy 대역일 때만 X-Forwarded-For를 해석한다.
 * 체인은 오른쪽부터 역순으로 따라가 첫 untrusted hop을 실제 클라이언트로 선택하므로,
 * 클라이언트가 임의로 앞에 붙인 XFF 값은 rate-limit identity에 영향을 주지 않는다.
 */
@Component
public class TrustedProxyClientIpResolver {

    private final TrustedProxyChain trustedProxyChain;

    public TrustedProxyClientIpResolver(FeedbackRateLimitProperties properties) {
        this.trustedProxyChain = new TrustedProxyChain(properties.trustedProxies());
    }

    public String resolve(HttpServletRequest request) {
        return trustedProxyChain.resolve(request);
    }
}
