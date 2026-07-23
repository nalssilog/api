package com.nalssilog.storage;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** 오브젝트 스토리지 공유 기능 — presigned PUT URL 발급 + 업로드 후 HEAD 검증 + 공개 URL 조립. 정책 판단은 호출 도메인이 한다. */
@Component
@RequiredArgsConstructor
public class ObjectStorage {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final StorageProperties properties;

    /** 업로드 후 HEAD 검증을 켤지(R2/MinIO 연결된 dev/prod 만 true). */
    public boolean isVerifyEnabled() {
        return properties.verifyUpload();
    }

    /** storageKey 의 실제 오브젝트 메타데이터. 없으면 empty (업로드 안 됐거나 잘못된 key). */
    public Optional<StoredObject> head(String storageKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(head -> head
                    .bucket(properties.r2().bucket())
                    .key(storageKey));

            return Optional.of(new StoredObject(response.contentType(), response.contentLength()));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }

            throw e;
        }
    }

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
