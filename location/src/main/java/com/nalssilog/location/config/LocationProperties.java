package com.nalssilog.location.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 지역 관련 설정. featuredAdminCodes 는 제보 0건일 때 인기 동네 자리에 노출할 대표 지역(순서 유지). */
@ConfigurationProperties("nalssilog.location")
public record LocationProperties(List<String> featuredAdminCodes) {

    public LocationProperties {
        featuredAdminCodes = featuredAdminCodes == null ? List.of() : featuredAdminCodes;
    }
}
