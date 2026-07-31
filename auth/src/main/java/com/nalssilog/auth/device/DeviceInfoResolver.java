package com.nalssilog.auth.device;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.common.web.TrustedProxyChain;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 요청에서 세션 기기 정보(표시용 이름 + IP)를 뽑아낸다.
 * User-Agent 파싱은 "로그인된 기기" 표시용 best-effort 이며(정밀 파서 아님), IP 는 프록시 헤더 우선.
 */
@Component
public class DeviceInfoResolver {

    private static final int MAX_IP_LENGTH = 45;   // IPv6 최대 길이
    private static final int MAX_NAME_LENGTH = 60;

    private final TrustedProxyChain trustedProxyChain;

    public DeviceInfoResolver(AuthProperties properties) {
        this.trustedProxyChain = new TrustedProxyChain(properties.mobile().trustedProxies());
    }

    public DeviceInfo resolve(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        return new DeviceInfo(deviceName(userAgent), resolveIp(request));
    }

    public String resolveIp(HttpServletRequest request) {
        String ip = trustedProxyChain.resolve(request);

        if (!StringUtils.hasText(ip)) {
            return "";
        }

        return ip.length() > MAX_IP_LENGTH ? ip.substring(0, MAX_IP_LENGTH) : ip;
    }

    private String deviceName(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "알 수 없는 기기";
        }

        String browser = browser(userAgent);
        String os = os(userAgent);
        String name = browser + " · " + os;

        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    private String browser(String ua) {
        if (ua.contains("Edg")) {
            return "Edge";
        }

        if (ua.contains("SamsungBrowser")) {
            return "Samsung Internet";
        }

        if (ua.contains("OPR") || ua.contains("Opera")) {
            return "Opera";
        }

        if (ua.contains("Firefox")) {
            return "Firefox";
        }

        if (ua.contains("Chrome")) {
            return "Chrome";
        }

        if (ua.contains("Safari")) {
            return "Safari";
        }

        return "브라우저";
    }

    private String os(String ua) {
        if (ua.contains("iPhone")) {
            return "iPhone";
        }

        if (ua.contains("iPad")) {
            return "iPad";
        }

        if (ua.contains("Android")) {
            return "Android";
        }

        if (ua.contains("Windows")) {
            return "Windows";
        }

        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
            return "Mac";
        }

        if (ua.contains("Linux")) {
            return "Linux";
        }

        return "기타";
    }

}
