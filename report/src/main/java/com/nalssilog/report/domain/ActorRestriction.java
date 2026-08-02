package com.nalssilog.report.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
import com.nalssilog.report.application.dto.ReportActor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "actor_restriction",
        indexes = @Index(
                name = "idx_actor_restriction_actor",
                columnList = "actor_type, actor_key, lifted_at, expires_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActorRestriction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType actorType;

    @Column(name = "actor_key", nullable = false, length = 64)
    private String actorKey;

    @Column(name = "source_report_id")
    private Long sourceReportId;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_by_member_id", nullable = false)
    private Long createdByMemberId;

    @Column(name = "lifted_at")
    private Instant liftedAt;

    @Column(name = "lifted_by_member_id")
    private Long liftedByMemberId;

    public static ActorRestriction create(
            ReportActor actor,
            Long sourceReportId,
            String reason,
            Instant expiresAt,
            Long adminMemberId
    ) {
        ActorRestriction restriction = new ActorRestriction();

        restriction.actorType = actor.type();
        restriction.actorKey = actor.actorKey();
        restriction.sourceReportId = sourceReportId;
        restriction.reason = reason;
        restriction.expiresAt = expiresAt;
        restriction.createdByMemberId = adminMemberId;

        return restriction;
    }

    public void lift(Long adminMemberId) {
        if (liftedAt == null) {
            liftedAt = Instant.now();
            liftedByMemberId = adminMemberId;
        }
    }
}
