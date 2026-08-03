package com.nalssilog.report.api.dto;

import com.nalssilog.report.domain.ReportFlagStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminProcessFlagRequest(
        @NotNull(message = "신고 처리 결과가 필요합니다.")
        ReportFlagStatus status,

        @Size(max = 500, message = "처리 메모는 500자 이하여야 합니다.")
        String note
) {
}
