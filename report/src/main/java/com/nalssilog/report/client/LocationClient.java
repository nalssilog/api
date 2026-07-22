package com.nalssilog.report.client;

import com.nalssilog.location.application.LocationService;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.report.application.dto.LocationSummary;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * report 모듈이 location 을 호출하는 창구. 프론트엔 위경도 제외 요약만 넘긴다.
 */
@Component
@RequiredArgsConstructor
public class LocationClient {

    private final LocationService locationService;

    public LocationSummary getLocation(Long locationId) {
        LocationInfo info = locationService.getLocation(locationId);

        return toSummary(info);
    }

    /**
     * 여러 지역을 일괄 조회해 id → 요약 맵으로 반환한다(여러 지역에 걸친 제보 목록 enrich 용).
     */
    public Map<Long, LocationSummary> getLocations(List<Long> locationIds) {
        return locationService.getLocations(locationIds).stream()
                .map(this::toSummary)
                .collect(Collectors.toMap(LocationSummary::id, Function.identity()));
    }

    private LocationSummary toSummary(LocationInfo info) {
        return new LocationSummary(info.id(), info.sido(), info.sigungu(), info.dong(), info.label());
    }
}
