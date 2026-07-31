package com.nalssilog.location.application.dto;

import com.nalssilog.location.domain.PopularRankMovement;
import java.time.Instant;
import java.util.List;

/**
 * 제보 모듈이 계산·저장한 인기 지역 순위 스냅샷.
 */
public record PopularLocationSnapshotData(
        Long snapshotId,
        Instant calculatedAt,
        Instant windowStartedAt,
        Instant windowEndedAt,
        String algorithmVersion,
        List<Rank> rankings
) {

    public PopularLocationSnapshotData {
        rankings = List.copyOf(rankings);
    }

    public record Rank(
            Long locationId,
            int rank,
            Integer previousRank,
            Integer rankChange,
            PopularRankMovement movement,
            long uniqueReporterCount,
            long reportCount,
            Instant latestReportAt
    ) {
    }
}
