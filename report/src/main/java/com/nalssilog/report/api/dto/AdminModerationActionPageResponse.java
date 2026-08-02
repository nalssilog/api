package com.nalssilog.report.api.dto;

import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.ModerationActionType;
import java.time.Instant;
import java.util.List;

public record AdminModerationActionPageResponse(
        List<Item> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public record Item(
            String id,
            ModerationActionType action,
            String adminMemberId,
            String reportId,
            ActorType targetActorType,
            String reason,
            Instant createdAt
    ) {
    }
}
