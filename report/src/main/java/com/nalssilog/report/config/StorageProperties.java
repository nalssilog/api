package com.nalssilog.report.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * 제보 이미지 정책 설정. R2 접속 정보·공개 도메인은 storage 모듈의 StorageProperties 가 공유로 들고 있고,
 * 여기선 제보 도메인 고유 정책(경로 prefix·presign TTL·장수·장당 최대 크기)만 둔다.
 * maxImagesPerReport(제품 정책=3)·maxImageBytes(장당 5MB)는 제보 이미지 정책과 일치시킨다.
 */
@ConfigurationProperties(prefix = "nalssilog.storage")
public record StorageProperties(
        @DefaultValue("reports") String keyPrefix,
        @DefaultValue("5m") Duration presignTtl,
        @DefaultValue("3") int maxImagesPerReport,
        @DefaultValue("5MB") DataSize maxImageBytes
) {
}
