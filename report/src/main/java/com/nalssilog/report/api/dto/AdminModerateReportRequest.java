package com.nalssilog.report.api.dto;

import com.nalssilog.report.domain.ReportModerationCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminModerateReportRequest(
        @NotNull(message = "제보 조치가 필요합니다.")
        ReportModerationCommand action,

        @NotBlank(message = "운영 조치 사유가 필요합니다.")
        @Size(max = 500, message = "운영 조치 사유는 500자 이하여야 합니다.")
        String reason
) {
}
