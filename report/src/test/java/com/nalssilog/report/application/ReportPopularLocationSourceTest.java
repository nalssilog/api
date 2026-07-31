package com.nalssilog.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.location.application.dto.PopularLocationSnapshotData;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960")
class ReportPopularLocationSourceTest {

    private final PopularLocationSnapshotService snapshotService =
            mock(PopularLocationSnapshotService.class);
    private final ReportPopularLocationSource source =
            new ReportPopularLocationSource(snapshotService);

    @Test
    void returnsLatestPersistedSnapshot() {
        Instant calculatedAt = Instant.parse("2026-07-30T06:00:00Z");
        PopularLocationSnapshotData snapshot = new PopularLocationSnapshotData(
                10L,
                calculatedAt,
                calculatedAt.minusSeconds(60),
                calculatedAt,
                "UNIQUE_REPORTERS_V1",
                List.of());

        when(snapshotService.latestOrRefreshAt(any(Instant.class))).thenReturn(snapshot);

        assertThat(source.latestSnapshot()).isSameAs(snapshot);
        verify(snapshotService).latestOrRefreshAt(any(Instant.class));
    }
}
