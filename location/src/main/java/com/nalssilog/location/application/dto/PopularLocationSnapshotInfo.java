package com.nalssilog.location.application.dto;

import com.nalssilog.location.domain.PopularRankMovement;
import java.time.Instant;
import java.util.List;

/**
 * 인기 순위 스냅샷에 화면 표시용 지역 정보를 결합한 내부 응답.
 */
public record PopularLocationSnapshotInfo(
        Long snapshotId,
        Instant calculatedAt,
        Instant windowStartedAt,
        Instant windowEndedAt,
        String algorithmVersion,
        List<Item> items
) {

    public PopularLocationSnapshotInfo {
        items = List.copyOf(items);
    }

    public record Item(
            int rank,
            Integer previousRank,
            Integer rankChange,
            PopularRankMovement movement,
            long uniqueReporterCount,
            long reportCount,
            Instant latestReportAt,
            LocationInfo location
    ) {
    }
}
