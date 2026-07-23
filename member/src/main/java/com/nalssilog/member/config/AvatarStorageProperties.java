package com.nalssilog.member.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * 커스텀 아바타 이미지 정책. R2 접속 정보·공개 도메인은 storage 모듈의 StorageProperties 가 공유로 들고 있고,
 * 여기선 아바타 도메인 고유 정책(경로 prefix·presign TTL·최대 크기)만 둔다.
 * 제보 이미지와 저장 경로·크기 정책이 달라 별도로 관리한다(프론트 권고 반영).
 */
@ConfigurationProperties(prefix = "nalssilog.storage.avatar")
public record AvatarStorageProperties(
        @DefaultValue("avatars") String keyPrefix,
        @DefaultValue("5m") Duration presignTtl,
        @DefaultValue("2MB") DataSize maxBytes
) {
}
