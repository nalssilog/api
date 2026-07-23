package com.nalssilog.auth.config;

import com.nalssilog.auth.application.dto.DeviceInfo;
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

    public DeviceInfo resolve(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        return new DeviceInfo(deviceName(userAgent), clientIp(request));
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

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = StringUtils.hasText(forwarded)
                ? forwarded.split(",")[0].strip()
                : request.getRemoteAddr();

        if (!StringUtils.hasText(ip)) {
            return "";
        }

        return ip.length() > MAX_IP_LENGTH ? ip.substring(0, MAX_IP_LENGTH) : ip;
    }
}
