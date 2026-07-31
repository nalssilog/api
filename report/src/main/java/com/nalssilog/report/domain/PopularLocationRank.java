package com.nalssilog.report.domain;

import com.nalssilog.location.domain.PopularRankMovement;
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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인기 지역 스냅샷의 개별 순위와 당시 집계 지표.
 */
@Entity
@Table(name = "popular_location_rank",
        indexes = @Index(
                name = "idx_popular_location_rank_snapshot",
                columnList = "snapshot_id, ranking_position"),
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_popular_location_rank_snapshot_position",
                        columnNames = {"snapshot_id", "ranking_position"}),
                @UniqueConstraint(
                        name = "uk_popular_location_rank_snapshot_location",
                        columnNames = {"snapshot_id", "location_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularLocationRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "ranking_position", nullable = false)
    private int position;

    @Column(name = "previous_ranking_position")
    private Integer previousPosition;

    @Column(name = "rank_change")
    private Integer rankChange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PopularRankMovement movement;

    @Column(name = "unique_reporter_count", nullable = false)
    private long uniqueReporterCount;

    @Column(name = "report_count", nullable = false)
    private long reportCount;

    @Column(name = "latest_report_at", nullable = false)
    private Instant latestReportAt;

    public static PopularLocationRank create(
            Long snapshotId,
            Long locationId,
            int position,
            Integer previousPosition,
            Integer rankChange,
            PopularRankMovement movement,
            long uniqueReporterCount,
            long reportCount,
            Instant latestReportAt
    ) {
        PopularLocationRank rank = new PopularLocationRank();

        rank.snapshotId = snapshotId;
        rank.locationId = locationId;
        rank.position = position;
        rank.previousPosition = previousPosition;
        rank.rankChange = rankChange;
        rank.movement = movement;
        rank.uniqueReporterCount = uniqueReporterCount;
        rank.reportCount = reportCount;
        rank.latestReportAt = latestReportAt;

        return rank;
    }
}
