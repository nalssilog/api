package com.nalssilog.report.api.dto;

import com.nalssilog.report.domain.ReportFlag;
import com.nalssilog.report.domain.ReportFlagReason;
import com.nalssilog.report.domain.ReportFlagStatus;
import java.time.Instant;

public record ReportFlagResponse(
        String id,
        String reportId,
        ReportFlagReason reason,
        ReportFlagStatus status,
        Instant createdAt
) {

    public static ReportFlagResponse from(ReportFlag flag) {
        return new ReportFlagResponse(
                String.valueOf(flag.getId()),
                String.valueOf(flag.getReport().getId()),
                flag.getReason(),
                flag.getStatus(),
                flag.getCreatedAt());
    }
}
