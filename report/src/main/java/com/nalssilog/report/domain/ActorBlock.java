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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "actor_block",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_actor_block_pair",
                columnNames = {"blocker_type", "blocker_key", "blocked_type", "blocked_key"}),
        indexes = @Index(name = "idx_actor_block_blocker", columnList = "blocker_type, blocker_key"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActorBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "blocker_type", nullable = false, length = 20)
    private ActorType blockerType;

    @Column(name = "blocker_key", nullable = false, length = 64)
    private String blockerKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "blocked_type", nullable = false, length = 20)
    private ActorType blockedType;

    @Column(name = "blocked_key", nullable = false, length = 64)
    private String blockedKey;

    public static ActorBlock create(ReportActor blocker, ReportActor blocked) {
        ActorBlock block = new ActorBlock();

        block.blockerType = blocker.type();
        block.blockerKey = blocker.actorKey();
        block.blockedType = blocked.type();
        block.blockedKey = blocked.actorKey();

        return block;
    }
}
