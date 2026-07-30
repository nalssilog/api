package com.nalssilog.report.application;

import com.nalssilog.location.application.dto.PopularLocationSnapshotData;
import com.nalssilog.location.config.PopularLocationProperties;
import com.nalssilog.location.domain.PopularRankMovement;
import com.nalssilog.report.application.dto.PopularLocationAggregate;
import com.nalssilog.report.domain.PopularLocationRank;
import com.nalssilog.report.domain.PopularLocationSnapshot;
import com.nalssilog.report.repository.PopularLocationRankJpaRepository;
import com.nalssilog.report.repository.PopularLocationSnapshotJpaRepository;
import com.nalssilog.report.repository.WeatherReportRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최근 제보 집계를 순위 스냅샷으로 저장하고 최신 스냅샷을 제공한다.
 */
@Service
@RequiredArgsConstructor
public class PopularLocationSnapshotService {

    private static final String ALGORITHM_VERSION = "UNIQUE_REPORTERS_V1";

    private final WeatherReportRepository reportRepository;
    private final PopularLocationSnapshotJpaRepository snapshotRepository;
    private final PopularLocationRankJpaRepository rankRepository;
    private final PopularLocationProperties properties;

    @Transactional
    public PopularLocationSnapshotData latestOrRefreshAt(Instant now) {
        PopularLocationSnapshot latest = snapshotRepository
                .findFirstByOrderByCalculatedAtDescIdDesc()
                .orElse(null);

        if (latest != null
                && !latest.getCalculatedAt().isBefore(now.minus(properties.snapshotInterval()))) {

            return snapshotData(latest);
        }

        return createSnapshot(now, latest);
    }

    @Transactional
    public PopularLocationSnapshotData captureAt(Instant calculatedAt) {
        PopularLocationSnapshot previous = snapshotRepository
                .findFirstByOrderByCalculatedAtDescIdDesc()
                .orElse(null);

        return createSnapshot(calculatedAt, previous);
    }

    private PopularLocationSnapshotData createSnapshot(
            Instant calculatedAt,
            PopularLocationSnapshot previous
    ) {
        Instant snapshotTime = calculatedAt.truncatedTo(ChronoUnit.SECONDS);
        Instant windowStartedAt = snapshotTime.minus(properties.window());
        List<PopularLocationAggregate> aggregates =
                reportRepository.findPopularLocationAggregates(
                        windowStartedAt,
                        snapshotTime,
                        properties.limit());
        Map<Long, Integer> previousPositions = previousPositions(previous);
        PopularLocationSnapshot snapshot = PopularLocationSnapshot.create(
                snapshotTime,
                windowStartedAt,
                snapshotTime,
                properties.limit(),
                ALGORITHM_VERSION);

        snapshotRepository.save(snapshot);

        List<PopularLocationRank> ranks = new ArrayList<>(aggregates.size());

        for (int index = 0; index < aggregates.size(); index++) {
            PopularLocationAggregate aggregate = aggregates.get(index);
            int position = index + 1;
            Integer previousPosition = previousPositions.get(aggregate.locationId());
            Integer rankChange = previousPosition == null
                    ? null
                    : previousPosition - position;

            ranks.add(PopularLocationRank.create(
                    snapshot.getId(),
                    aggregate.locationId(),
                    position,
                    previousPosition,
                    rankChange,
                    movement(rankChange),
                    aggregate.uniqueReporterCount(),
                    aggregate.reportCount(),
                    aggregate.latestReportAt()));
        }

        rankRepository.saveAll(ranks);

        return snapshotData(snapshot, ranks);
    }

    private Map<Long, Integer> previousPositions(PopularLocationSnapshot previous) {
        if (!isComparable(previous)) {

            return Map.of();
        }

        return rankRepository.findAllBySnapshotIdOrderByPositionAsc(previous.getId()).stream()
                .collect(Collectors.toMap(
                        PopularLocationRank::getLocationId,
                        PopularLocationRank::getPosition));
    }

    private boolean isComparable(PopularLocationSnapshot previous) {
        if (previous == null
                || !ALGORITHM_VERSION.equals(previous.getAlgorithmVersion())
                || previous.getRankingLimit() != properties.limit()) {

            return false;
        }

        Duration previousWindow = Duration.between(
                previous.getWindowStartedAt(),
                previous.getWindowEndedAt());

        return previousWindow.equals(properties.window());
    }

    private PopularLocationSnapshotData snapshotData(PopularLocationSnapshot snapshot) {
        List<PopularLocationRank> ranks =
                rankRepository.findAllBySnapshotIdOrderByPositionAsc(snapshot.getId());

        return snapshotData(snapshot, ranks);
    }

    private PopularLocationSnapshotData snapshotData(
            PopularLocationSnapshot snapshot,
            List<PopularLocationRank> ranks
    ) {
        List<PopularLocationSnapshotData.Rank> rankings = ranks.stream()
                .map(rank -> new PopularLocationSnapshotData.Rank(
                        rank.getLocationId(),
                        rank.getPosition(),
                        rank.getPreviousPosition(),
                        rank.getRankChange(),
                        rank.getMovement(),
                        rank.getUniqueReporterCount(),
                        rank.getReportCount(),
                        rank.getLatestReportAt()))
                .toList();

        return new PopularLocationSnapshotData(
                snapshot.getId(),
                snapshot.getCalculatedAt(),
                snapshot.getWindowStartedAt(),
                snapshot.getWindowEndedAt(),
                snapshot.getAlgorithmVersion(),
                rankings);
    }

    private static PopularRankMovement movement(Integer rankChange) {
        if (rankChange == null) {

            return PopularRankMovement.NEW;
        }
        if (rankChange > 0) {

            return PopularRankMovement.UP;
        }
        if (rankChange < 0) {

            return PopularRankMovement.DOWN;
        }

        return PopularRankMovement.SAME;
    }
}
