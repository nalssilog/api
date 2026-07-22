package com.nalssilog.member.client;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.AvatarPresign;
import com.nalssilog.member.config.AvatarStorageProperties;
import com.nalssilog.member.domain.MemberErrorCode;
import com.nalssilog.storage.ObjectStorage;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 커스텀 아바타 창구. 정책(형식·크기·경로) 검증 + storage_key 생성, presign·공개URL 은 공유 {@link ObjectStorage} 위임.
 * key 에 memberId 를 박아(avatars/{memberId}/…) 변경 시 본인 발급 여부를 검증한다.
 */
@Component
@RequiredArgsConstructor
public class AvatarStorageClient {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final ObjectStorage objectStorage;
    private final AvatarStorageProperties properties;

    public AvatarPresign presign(Long memberId, String contentType, long size) {
        String normalized = normalize(contentType);
        String extension = EXTENSION_BY_CONTENT_TYPE.get(normalized);

        if (extension == null) {
            throw new NalssiLogException(MemberErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }

        if (size <= 0 || size > properties.maxBytes().toBytes()) {
            throw new NalssiLogException(MemberErrorCode.IMAGE_TOO_LARGE);
        }

        String storageKey = generateKey(memberId, extension);
        String uploadUrl = objectStorage.presignPut(storageKey, normalized, properties.presignTtl());

        return new AvatarPresign(storageKey, uploadUrl, normalized, size);
    }

    /** key 가 이 회원에게 발급된 것인지 검증(avatars/{memberId}/ 로 시작). */
    public void validateKey(Long memberId, String storageKey) {
        if (storageKey == null || !storageKey.startsWith(memberPrefix(memberId))) {
            throw new NalssiLogException(MemberErrorCode.INVALID_IMAGE_KEY);
        }
    }

    public String toPublicUrl(String storageKey) {
        return objectStorage.publicUrl(storageKey);
    }

    private String generateKey(Long memberId, String extension) {
        return "%s%s.%s".formatted(memberPrefix(memberId), UUID.randomUUID(), extension);
    }

    private String memberPrefix(Long memberId) {
        return "%s/%d/".formatted(properties.keyPrefix(), memberId);
    }

    private String normalize(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase();
    }
}
