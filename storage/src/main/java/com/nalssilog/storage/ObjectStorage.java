package com.nalssilog.storage;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** 오브젝트 스토리지 공유 기능 — presigned PUT URL 발급 + 공개 URL 조립. 형식·크기·경로 검증은 호출 도메인이 한다. */
@Component
@RequiredArgsConstructor
public class ObjectStorage {

    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    public String presignPut(String storageKey, String contentType, Duration ttl) {
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(put -> put
                        .bucket(properties.r2().bucket())
                        .key(storageKey)
                        .contentType(contentType))
                .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        return presigned.url().toString();
    }

    /** storageKey → 공개 URL. publicBaseUrl 미설정(placeholder)이면 key 그대로 반환. */
    public String publicUrl(String storageKey) {
        if (storageKey == null || properties.publicBaseUrl() == null) {
            return storageKey;
        }

        String base = properties.publicBaseUrl();

        return base.endsWith("/") ? base + storageKey : base + "/" + storageKey;
    }
}
