package com.nalssilog.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 특정 계산 시점의 인기 지역 순위 헤더.
 */
@Entity
@Table(name = "popular_location_snapshot",
        indexes = @Index(
                name = "idx_popular_location_snapshot_calculated",
                columnList = "calculated_at"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_popular_location_snapshot_calculation",
                columnNames = {
                        "calculated_at",
                        "window_started_at",
                        "ranking_limit",
                        "algorithm_version"
                }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularLocationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "window_ended_at", nullable = false)
    private Instant windowEndedAt;

    @Column(name = "ranking_limit", nullable = false)
    private int rankingLimit;

    @Column(name = "algorithm_version", nullable = false, length = 40)
    private String algorithmVersion;

    public static PopularLocationSnapshot create(
            Instant calculatedAt,
            Instant windowStartedAt,
            Instant windowEndedAt,
            int rankingLimit,
            String algorithmVersion
    ) {
        PopularLocationSnapshot snapshot = new PopularLocationSnapshot();

        snapshot.calculatedAt = calculatedAt;
        snapshot.windowStartedAt = windowStartedAt;
        snapshot.windowEndedAt = windowEndedAt;
        snapshot.rankingLimit = rankingLimit;
        snapshot.algorithmVersion = algorithmVersion;

        return snapshot;
    }
}
