package com.nalssilog.report.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * presigned 업로드 발급 요청. 업로드할 이미지별 contentType(예: image/jpeg)과 size(바이트).
 * size 는 발급 시 장당 최대 용량(5MB) 선언검증에 쓰인다. 장수 초과는 서비스에서 TOO_MANY_IMAGES.
 */
public record PresignRequest(
        @NotEmpty(message = "업로드할 이미지 정보가 필요합니다.")
        @Valid
        List<Image> images
) {

    public record Image(
            @NotBlank(message = "이미지 형식이 필요합니다.")
            String contentType,

            @NotNull(message = "이미지 크기가 필요합니다.")
            @Positive(message = "이미지 크기가 올바르지 않습니다.")
            Long size
    ) {
    }
}
