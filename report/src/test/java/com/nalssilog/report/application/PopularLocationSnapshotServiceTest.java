package com.nalssilog.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.location.config.PopularLocationProperties;
import com.nalssilog.location.domain.PopularRankMovement;
import com.nalssilog.report.application.dto.PopularLocationAggregate;
import com.nalssilog.report.domain.PopularLocationRank;
import com.nalssilog.report.domain.PopularLocationSnapshot;
import com.nalssilog.report.repository.PopularLocationRankJpaRepository;
import com.nalssilog.report.repository.PopularLocationSnapshotJpaRepository;
import com.nalssilog.report.repository.PopularLocationSnapshotLockRepository;
import com.nalssilog.report.repository.WeatherReportRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

@SuppressWarnings("java:S5960")
class PopularLocationSnapshotServiceTest {

    private static final PopularLocationProperties PROPERTIES =
            new PopularLocationProperties(
                    Duration.ofDays(7),
                    20,
                    Duration.ofMinutes(10));

    private final WeatherReportRepository reportRepository =
            mock(WeatherReportRepository.class);
    private final PopularLocationSnapshotJpaRepository snapshotRepository =
            mock(PopularLocationSnapshotJpaRepository.class);
    private final PopularLocationRankJpaRepository rankRepository =
            mock(PopularLocationRankJpaRepository.class);
    private final PopularLocationSnapshotLockRepository lockRepository =
            mock(PopularLocationSnapshotLockRepository.class);
    private final PopularLocationSnapshotService service =
            new PopularLocationSnapshotService(
                    reportRepository,
                    snapshotRepository,
                    rankRepository,
                    lockRepository,
                    PROPERTIES);

    @Test
    void snapshotWritesAlwaysUseIndependentTransactions() throws NoSuchMethodException {
        AnnotationTransactionAttributeSource attributeSource =
                new AnnotationTransactionAttributeSource();
        var refreshAttribute = attributeSource.getTransactionAttribute(
                PopularLocationSnapshotService.class.getMethod(
                        "latestOrRefreshAt",
                        Instant.class),
                PopularLocationSnapshotService.class);
        var captureAttribute = attributeSource.getTransactionAttribute(
                PopularLocationSnapshotService.class.getMethod(
                        "captureAt",
                        Instant.class),
                PopularLocationSnapshotService.class);

        assertThat(refreshAttribute).isNotNull();
        assertThat(refreshAttribute.getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(refreshAttribute.isReadOnly()).isFalse();
        assertThat(captureAttribute).isNotNull();
        assertThat(captureAttribute.getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(captureAttribute.isReadOnly()).isFalse();
    }

    @Test
    void createsFirstSnapshotWithNewMovementsAndDetailedMetrics() {
        Instant calculatedAt = Instant.parse("2026-07-30T06:00:00Z");
        Instant windowStartedAt = calculatedAt.minus(Duration.ofDays(7));

        when(snapshotRepository.findFirstByOrderByCalculatedAtDescIdDesc())
                .thenReturn(Optional.empty());
        when(reportRepository.findPopularLocationAggregates(windowStartedAt, calculatedAt, 20))
                .thenReturn(List.of(
                        new PopularLocationAggregate(
                                11L,
                                4,
                                7,
                                calculatedAt.minusSeconds(60)),
                        new PopularLocationAggregate(
                                12L,
                                2,
                                3,
                                calculatedAt.minusSeconds(120))));
        assignSnapshotId(100L);

        var result = service.captureAt(calculatedAt);

        assertThat(result.snapshotId()).isEqualTo(100L);
        assertThat(result.windowStartedAt()).isEqualTo(windowStartedAt);
        assertThat(result.rankings()).hasSize(2);
        assertThat(result.rankings().getFirst().rank()).isEqualTo(1);
        assertThat(result.rankings().getFirst().previousRank()).isNull();
        assertThat(result.rankings().getFirst().rankChange()).isNull();
        assertThat(result.rankings().getFirst().movement()).isEqualTo(PopularRankMovement.NEW);
        assertThat(result.rankings().getFirst().uniqueReporterCount()).isEqualTo(4);
        assertThat(result.rankings().getFirst().reportCount()).isEqualTo(7);
        verify(rankRepository).saveAll(any());
    }

    @Test
    void comparesNewSnapshotWithPreviousRanking() {
        Instant calculatedAt = Instant.parse("2026-07-30T06:00:00Z");
        PopularLocationSnapshot previous = snapshot(
                90L,
                calculatedAt.minus(Duration.ofMinutes(10)));
        List<PopularLocationRank> previousRanks = List.of(
                rank(90L, 11L, 1),
                rank(90L, 12L, 2));

        when(snapshotRepository.findFirstByOrderByCalculatedAtDescIdDesc())
                .thenReturn(Optional.of(previous));
        when(rankRepository.findAllBySnapshotIdOrderByPositionAsc(90L))
                .thenReturn(previousRanks);
        when(reportRepository.findPopularLocationAggregates(
                calculatedAt.minus(Duration.ofDays(7)),
                calculatedAt,
                20))
                .thenReturn(List.of(
                        aggregate(12L, calculatedAt),
                        aggregate(11L, calculatedAt),
                        aggregate(13L, calculatedAt)));
        assignSnapshotId(100L);

        var result = service.captureAt(calculatedAt);

        assertThat(result.rankings())
                .extracting(
                        ranking -> ranking.locationId(),
                        ranking -> ranking.previousRank(),
                        ranking -> ranking.rankChange(),
                        ranking -> ranking.movement())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                12L, 2, 1, PopularRankMovement.UP),
                        org.assertj.core.groups.Tuple.tuple(
                                11L, 1, -1, PopularRankMovement.DOWN),
                        org.assertj.core.groups.Tuple.tuple(
                                13L, null, null, PopularRankMovement.NEW));
    }

    @Test
    void reusesFreshSnapshotWithoutRunningAggregation() {
        Instant now = Instant.parse("2026-07-30T06:00:00Z");
        PopularLocationSnapshot latest = snapshot(
                100L,
                now.minus(Duration.ofMinutes(5)));
        PopularLocationRank storedRank = rank(100L, 11L, 1);

        when(snapshotRepository.findFirstByOrderByCalculatedAtDescIdDesc())
                .thenReturn(Optional.of(latest));
        when(rankRepository.findAllBySnapshotIdOrderByPositionAsc(100L))
                .thenReturn(List.of(storedRank));

        var result = service.latestOrRefreshAt(now);

        assertThat(result.snapshotId()).isEqualTo(100L);
        assertThat(result.rankings()).singleElement()
                .satisfies(ranking -> assertThat(ranking.locationId()).isEqualTo(11L));
        verify(reportRepository, never())
                .findPopularLocationAggregates(any(), any(), anyInt());
        verify(snapshotRepository, never()).saveAndFlush(any());
        verify(lockRepository, never()).acquire();
    }

    @Test
    void rechecksLatestSnapshotAfterAcquiringDistributedLock() {
        Instant now = Instant.parse("2026-07-30T06:05:00Z");
        PopularLocationSnapshot concurrentSnapshot = snapshot(
                100L,
                Instant.parse("2026-07-30T06:00:00Z"));
        PopularLocationRank storedRank = rank(100L, 11L, 1);

        when(snapshotRepository.findFirstByOrderByCalculatedAtDescIdDesc())
                .thenReturn(
                        Optional.empty(),
                        Optional.of(concurrentSnapshot));
        when(rankRepository.findAllBySnapshotIdOrderByPositionAsc(100L))
                .thenReturn(List.of(storedRank));

        var result = service.latestOrRefreshAt(now);

        assertThat(result.snapshotId()).isEqualTo(100L);
        verify(lockRepository).acquire();
        verify(reportRepository, never())
                .findPopularLocationAggregates(any(), any(), anyInt());
    }

    @Test
    void reusesSnapshotFromSameCalculationBucket() {
        Instant requestedAt = Instant.parse("2026-07-30T06:07:00Z");
        PopularLocationSnapshot existing = snapshot(
                100L,
                Instant.parse("2026-07-30T06:00:00Z"));
        PopularLocationRank storedRank = rank(100L, 11L, 1);

        when(snapshotRepository.findFirstByOrderByCalculatedAtDescIdDesc())
                .thenReturn(Optional.of(existing));
        when(rankRepository.findAllBySnapshotIdOrderByPositionAsc(100L))
                .thenReturn(List.of(storedRank));

        var result = service.captureAt(requestedAt);

        assertThat(result.snapshotId()).isEqualTo(100L);
        assertThat(result.calculatedAt())
                .isEqualTo(Instant.parse("2026-07-30T06:00:00Z"));
        verify(lockRepository).acquire();
        verify(reportRepository, never())
                .findPopularLocationAggregates(any(), any(), anyInt());
        verify(snapshotRepository, never()).saveAndFlush(any());
    }

    @Test
    void startsNewMovementBaselineWhenAlgorithmChanges() {
        Instant calculatedAt = Instant.parse("2026-07-30T06:00:00Z");
        PopularLocationSnapshot previous = snapshot(
                90L,
                calculatedAt.minus(Duration.ofMinutes(10)),
                "RECENT_ACTIVITY_V0");

        when(snapshotRepository.findFirstByOrderByCalculatedAtDescIdDesc())
                .thenReturn(Optional.of(previous));
        when(reportRepository.findPopularLocationAggregates(
                calculatedAt.minus(Duration.ofDays(7)),
                calculatedAt,
                20))
                .thenReturn(List.of(aggregate(11L, calculatedAt)));
        assignSnapshotId(100L);

        var result = service.captureAt(calculatedAt);

        assertThat(result.rankings()).singleElement()
                .satisfies(ranking -> {
                    assertThat(ranking.previousRank()).isNull();
                    assertThat(ranking.rankChange()).isNull();
                    assertThat(ranking.movement()).isEqualTo(PopularRankMovement.NEW);
                });
        verify(rankRepository, never()).findAllBySnapshotIdOrderByPositionAsc(90L);
    }

    private void assignSnapshotId(Long id) {
        when(snapshotRepository.saveAndFlush(any(PopularLocationSnapshot.class)))
                .thenAnswer(invocation -> {
                    PopularLocationSnapshot snapshot = invocation.getArgument(0);

                    ReflectionTestUtils.setField(snapshot, "id", id);

                    return snapshot;
                });
    }

    private static PopularLocationSnapshot snapshot(Long id, Instant calculatedAt) {
        return snapshot(id, calculatedAt, "UNIQUE_REPORTERS_V1");
    }

    private static PopularLocationSnapshot snapshot(
            Long id,
            Instant calculatedAt,
            String algorithmVersion
    ) {
        PopularLocationSnapshot snapshot = PopularLocationSnapshot.create(
                calculatedAt,
                calculatedAt.minus(Duration.ofDays(7)),
                calculatedAt,
                20,
                algorithmVersion);

        ReflectionTestUtils.setField(snapshot, "id", id);

        return snapshot;
    }

    private static PopularLocationRank rank(Long snapshotId, Long locationId, int position) {
        return PopularLocationRank.create(
                snapshotId,
                locationId,
                position,
                position,
                0,
                PopularRankMovement.SAME,
                1,
                1,
                Instant.parse("2026-07-30T05:00:00Z"));
    }

    private static PopularLocationAggregate aggregate(Long locationId, Instant calculatedAt) {
        return new PopularLocationAggregate(
                locationId,
                1,
                1,
                calculatedAt.minusSeconds(60));
    }
}
