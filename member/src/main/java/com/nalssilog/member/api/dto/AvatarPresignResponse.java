package com.nalssilog.member.api.dto;

import com.nalssilog.member.application.dto.AvatarPresign;

/**
 * 커스텀 아바타 presigned 업로드 발급 응답. 프론트는 uploadUrl 로 파일을 PUT 한 뒤,
 * PATCH /api/members/me/avatar 에 {type: "CUSTOM", value: storageKey} 로 보낸다.
 */
public record AvatarPresignResponse(String storageKey, String uploadUrl, String contentType, long size) {

    public static AvatarPresignResponse from(AvatarPresign presign) {
        return new AvatarPresignResponse(
                presign.storageKey(), presign.uploadUrl(), presign.contentType(), presign.size());
    }
}
