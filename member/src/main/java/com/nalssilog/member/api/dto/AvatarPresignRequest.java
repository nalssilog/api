package com.nalssilog.member.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 커스텀 아바타 presigned 업로드 발급 요청. contentType(image/jpeg 등)과 size(바이트).
 * size 는 발급 시 최대 용량(2MB) 선언검증에 쓰인다.
 */
public record AvatarPresignRequest(
        @NotBlank(message = "이미지 형식이 필요합니다.")
        String contentType,

        @NotNull(message = "이미지 크기가 필요합니다.")
        @Positive(message = "이미지 크기가 올바르지 않습니다.")
        Long size
) {
}
