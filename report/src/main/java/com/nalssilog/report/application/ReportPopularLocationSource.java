package com.nalssilog.report.application;

import com.nalssilog.location.application.PopularLocationSource;
import com.nalssilog.location.application.dto.PopularLocationSnapshotData;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * location의 인기 지역 port를 최근 제보 순위 스냅샷 기반으로 구현한다.
 */
@Component
@RequiredArgsConstructor
public class ReportPopularLocationSource implements PopularLocationSource {

    private final PopularLocationSnapshotService snapshotService;

    @Override
    public PopularLocationSnapshotData latestSnapshot() {

        return snapshotService.latestOrRefreshAt(Instant.now());
    }
}
