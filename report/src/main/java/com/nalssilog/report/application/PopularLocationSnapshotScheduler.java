package com.nalssilog.report.application;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopularLocationSnapshotScheduler {

    private final PopularLocationSnapshotService snapshotService;

    @Scheduled(
            fixedDelayString = "${nalssilog.location.popular.snapshot-interval:10m}",
            initialDelayString = "${nalssilog.location.popular.snapshot-initial-delay:10s}")
    public void refreshSnapshot() {
        snapshotService.latestOrRefreshAt(Instant.now());
    }
}
