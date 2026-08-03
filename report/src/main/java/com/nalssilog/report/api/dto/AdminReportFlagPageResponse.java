package com.nalssilog.report.api.dto;

import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.ModerationStatus;
import com.nalssilog.report.domain.ReportFlagReason;
import com.nalssilog.report.domain.ReportFlagStatus;
import java.time.Instant;
import java.util.List;

public record AdminReportFlagPageResponse(
        List<Item> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public record Item(
            String id,
            String reportId,
            ReportFlagReason reason,
            String detail,
            ReportFlagStatus status,
            ActorType reporterType,
            Instant createdAt,
            Instant processedAt,
            String processedByMemberId,
            String resolutionNote,
            Report report
    ) {
    }

    public record Report(
            String locationId,
            ActorType authorType,
            String authorMemberId,
            String comment,
            List<String> imageUrls,
            ModerationStatus moderationStatus,
            Instant createdAt
    ) {
    }
}
