package com.nalssilog.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** CORS 허용 출처(프론트 origin). env 별 리스트 — 로컬/dev/prod 실제 프론트 도메인만. 패턴(*.vercel.app 등) 허용. */
@ConfigurationProperties(prefix = "nalssilog.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
    }
}
