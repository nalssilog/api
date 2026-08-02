package com.nalssilog.report.api.dto;

import com.nalssilog.report.domain.ModerationStatus;

public record AdminReportModerationResponse(String reportId, ModerationStatus status) {
}
