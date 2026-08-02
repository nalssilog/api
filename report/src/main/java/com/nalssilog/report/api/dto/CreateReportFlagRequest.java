package com.nalssilog.report.api.dto;

import com.nalssilog.report.domain.ReportFlagReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportFlagRequest(
        @NotNull(message = "신고 사유가 필요합니다.")
        ReportFlagReason reason,

        @Size(max = 500, message = "신고 상세 내용은 500자 이하여야 합니다.")
        String detail
) {
}
