package com.nalssilog.report.api.dto;

import com.nalssilog.report.domain.ActorRestriction;
import com.nalssilog.report.domain.ActorType;
import java.time.Instant;

public record AdminActorRestrictionResponse(
        String id,
        String sourceReportId,
        ActorType actorType,
        String reason,
        Instant expiresAt,
        Instant liftedAt,
        boolean active
) {

    public static AdminActorRestrictionResponse from(ActorRestriction restriction) {
        return new AdminActorRestrictionResponse(
                String.valueOf(restriction.getId()),
                restriction.getSourceReportId() == null
                        ? null
                        : String.valueOf(restriction.getSourceReportId()),
                restriction.getActorType(),
                restriction.getReason(),
                restriction.getExpiresAt(),
                restriction.getLiftedAt(),
                restriction.getLiftedAt() == null
                        && (restriction.getExpiresAt() == null || restriction.getExpiresAt().isAfter(Instant.now())));
    }
}
