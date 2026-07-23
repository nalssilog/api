package com.nalssilog.report.client;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.application.dto.PresignedUpload;
import com.nalssilog.report.config.StorageProperties;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.storage.ObjectStorage;
import com.nalssilog.storage.StoredObject;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 제보 이미지 창구. 정책(형식·크기·장수·경로) 검증 + storage_key 생성, presign·공개URL 은 공유 {@link ObjectStorage} 위임. */
@Component
@RequiredArgsConstructor
public class ImageStorageClient {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy/MM");
    // 제품 정책상 jpeg/png/webp 만 허용 (gif 제외).
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final ObjectStorage objectStorage;
    private final StorageProperties properties;

    // Content-Length 는 서명에 안 박는다(브라우저가 자동 설정, 서명 시 R2 SigV4/CORS 깨질 수 있음). 실제 크기강제는 R2 연결 후 HEAD.
    public PresignedUpload presignUpload(String contentType, long size) {
        String normalized = normalize(contentType);
        String extension = EXTENSION_BY_CONTENT_TYPE.get(normalized);

        if (extension == null) {
            throw new NalssiLogException(ReportErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }

        if (size <= 0 || size > properties.maxImageBytes().toBytes()) {
            throw new NalssiLogException(ReportErrorCode.IMAGE_TOO_LARGE);
        }

        String storageKey = generateKey(extension);
        String uploadUrl = objectStorage.presignPut(storageKey, normalized, properties.presignTtl());

        return new PresignedUpload(storageKey, uploadUrl, normalized, size);
    }

    public void validateImageCount(int count) {
        if (count > properties.maxImagesPerReport()) {
            throw new NalssiLogException(ReportErrorCode.TOO_MANY_IMAGES);
        }
    }

    public String toPublicUrl(String storageKey) {
        return objectStorage.publicUrl(storageKey);
    }

    public void delete(String storageKey) {
        if (objectStorage.isVerifyEnabled()) {
            objectStorage.delete(storageKey);
        }
    }

    /** 넘어온 key 가 우리가 발급한 prefix 형식인지 검증(임의 key 저장 방지). */
    public void validateKey(String storageKey) {
        if (storageKey == null || !storageKey.startsWith(properties.keyPrefix() + "/")) {
            throw new NalssiLogException(ReportErrorCode.INVALID_IMAGE_KEY);
        }
    }

    /** 업로드 후 R2 HEAD 로 실제 존재·타입·크기 검증(클라 선언값 우회 방지). verifyUpload off(local)면 스킵. */
    public void verifyUploaded(String storageKey) {
        if (!objectStorage.isVerifyEnabled()) {
            return;
        }

        StoredObject object = objectStorage.head(storageKey)
                .orElseThrow(() -> new NalssiLogException(ReportErrorCode.IMAGE_NOT_FOUND));

        if (!EXTENSION_BY_CONTENT_TYPE.containsKey(normalize(object.contentType()))) {
            throw new NalssiLogException(ReportErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }

        if (object.contentLength() > properties.maxImageBytes().toBytes()) {
            throw new NalssiLogException(ReportErrorCode.IMAGE_TOO_LARGE);
        }
    }

    private String generateKey(String extension) {
        String datePath = LocalDate.now(ZoneOffset.UTC).format(YEAR_MONTH);

        return "%s/%s/%s.%s".formatted(properties.keyPrefix(), datePath, UUID.randomUUID(), extension);
    }

    private String normalize(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase();
    }
}
