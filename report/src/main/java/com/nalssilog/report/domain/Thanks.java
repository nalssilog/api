package com.nalssilog.report.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
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

/**
 * 감사해요. 카운트 컬럼 없이 행 하나 = 한 번의 감사. (reportId, actorType, actorKey) 유니크로 중복 방지.
 * 목록 조회 시 GROUP BY 배치 count 로 N+1 없이 집계한다.
 */
@Entity
@Table(name = "thanks",
        indexes = @Index(name = "idx_thanks_report", columnList = "report_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_thanks_report_actor",
                columnNames = {"report_id", "actor_type", "actor_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Thanks extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType actorType;

    @Column(name = "actor_key", nullable = false, length = 64)
    private String actorKey;

    public static Thanks create(Long reportId, ActorType actorType, String actorKey) {
        Thanks thanks = new Thanks();

        thanks.reportId = reportId;
        thanks.actorType = actorType;
        thanks.actorKey = actorKey;

        return thanks;
    }
}
