package com.nalssilog.location.api.dto;

import com.nalssilog.location.application.dto.PopularLocationSnapshotInfo;
import com.nalssilog.location.domain.PopularRankMovement;
import java.time.Instant;
import java.util.List;

/**
 * 동일 계산 시점의 인기 지역 top 20 전체 응답.
 * 프론트는 {@code pageSize} 단위로 나누어 표시한다.
 */
public record PopularLocationsResponse(
        String snapshotId,
        Instant calculatedAt,
        Instant windowStartedAt,
        Instant windowEndedAt,
        String algorithmVersion,
        int pageSize,
        long totalElements,
        int totalPages,
        List<Ranking> items
) {

    private static final int PAGE_SIZE = 5;

    public PopularLocationsResponse {
        items = List.copyOf(items);
    }

    public static PopularLocationsResponse from(PopularLocationSnapshotInfo snapshot) {
        List<Ranking> items = snapshot.items().stream()
                .map(Ranking::from)
                .toList();
        int totalPages = items.isEmpty()
                ? 0
                : (items.size() + PAGE_SIZE - 1) / PAGE_SIZE;

        return new PopularLocationsResponse(
                String.valueOf(snapshot.snapshotId()),
                snapshot.calculatedAt(),
                snapshot.windowStartedAt(),
                snapshot.windowEndedAt(),
                snapshot.algorithmVersion(),
                PAGE_SIZE,
                items.size(),
                totalPages,
                items);
    }

    public record Ranking(
            int rank,
            Integer previousRank,
            Integer rankChange,
            PopularRankMovement movement,
            long uniqueReporterCount,
            long reportCount,
            Instant latestReportAt,
            LocationResponse location
    ) {

        private static Ranking from(PopularLocationSnapshotInfo.Item item) {

            return new Ranking(
                    item.rank(),
                    item.previousRank(),
                    item.rankChange(),
                    item.movement(),
                    item.uniqueReporterCount(),
                    item.reportCount(),
                    item.latestReportAt(),
                    LocationResponse.from(item.location()));
        }
    }
}
