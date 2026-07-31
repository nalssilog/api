package com.nalssilog.location.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.application.dto.PopularLocationSnapshotInfo;
import com.nalssilog.location.domain.PopularRankMovement;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960")
class PopularLocationsResponseTest {

    @Test
    void calculatesFrontendPagesWhileReturningOneAtomicSnapshot() {
        Instant calculatedAt = Instant.parse("2026-07-30T06:00:00Z");
        List<PopularLocationSnapshotInfo.Item> items = IntStream.rangeClosed(1, 6)
                .mapToObj(rank -> new PopularLocationSnapshotInfo.Item(
                        rank,
                        rank,
                        0,
                        PopularRankMovement.SAME,
                        1,
                        1,
                        calculatedAt.minusSeconds(rank),
                        new LocationInfo(
                                (long) rank,
                                "서울특별시",
                                "강남구",
                                "동" + rank,
                                null,
                                null)))
                .toList();
        PopularLocationSnapshotInfo snapshot = new PopularLocationSnapshotInfo(
                10L,
                calculatedAt,
                calculatedAt.minusSeconds(7 * 24 * 60 * 60),
                calculatedAt,
                "UNIQUE_REPORTERS_V1",
                items);

        PopularLocationsResponse response = PopularLocationsResponse.from(snapshot);

        assertThat(response.pageSize()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(6);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.items()).hasSize(6);
    }
}
