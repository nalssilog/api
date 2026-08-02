package com.nalssilog.report.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AdminRestrictAuthorRequest(
        @NotBlank(message = "작성 제한 사유가 필요합니다.")
        @Size(max = 500, message = "작성 제한 사유는 500자 이하여야 합니다.")
        String reason,

        /** null이면 무기한, 값이 있으면 해당 시각까지 제한. */
        Instant expiresAt
) {
}
