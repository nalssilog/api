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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "moderation_action",
        indexes = {
                @Index(name = "idx_moderation_action_report", columnList = "report_id, created_at"),
                @Index(name = "idx_moderation_action_admin", columnList = "admin_member_id, created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModerationAction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private ModerationActionType actionType;

    @Column(name = "admin_member_id", nullable = false)
    private Long adminMemberId;

    @Column(name = "report_id")
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_actor_type", length = 20)
    private ActorType targetActorType;

    @Column(name = "target_actor_key", length = 64)
    private String targetActorKey;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    public static ModerationAction create(
            ModerationActionType type,
            Long adminMemberId,
            Long reportId,
            ReportActor target,
            String reason
    ) {
        ModerationAction action = new ModerationAction();

        action.actionType = type;
        action.adminMemberId = adminMemberId;
        action.reportId = reportId;
        action.targetActorType = target == null ? null : target.type();
        action.targetActorKey = target == null ? null : target.actorKey();
        action.reason = reason;

        return action;
    }
}
