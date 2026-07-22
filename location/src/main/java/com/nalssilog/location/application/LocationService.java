package com.nalssilog.location.application;

import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.config.LocationProperties;
import com.nalssilog.location.repository.LocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지역 검색·조회·역지오코딩·인기 지역 유스케이스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private static final int POPULAR_SIZE = 5;

    private final LocationRepository locationRepository;
    private final PopularLocationSource popularLocationSource;
    private final LocationProperties properties;

    public List<LocationInfo> search(String keyword) {
        return locationRepository.searchByKeyword(keyword.strip());
    }

    public LocationInfo getLocation(Long locationId) {
        return locationRepository.getById(locationId);
    }

    /**
     * 여러 지역 일괄 조회. 없는 id 는 조용히 제외한다(제보 목록의 지역 enrich 배치용).
     */
    public List<LocationInfo> getLocations(List<Long> locationIds) {
        return locationRepository.findByIds(locationIds);
    }

    /** GPS 좌표 → 행정동. 현재는 중심좌표 기준 평면 최근접(카카오 역지오코딩으로 교체 가능). */
    public LocationInfo reverseGeocode(double latitude, double longitude) {
        return locationRepository.findNearest(latitude, longitude);
    }

    /** 인기 동네 top5(제보 활동 기반, report 가 PopularLocationSource 로 공급). 제보 없으면 설정된 대표 지역 fallback. */
    public List<LocationInfo> getPopular() {
        List<Long> popularIds = popularLocationSource.topLocationIds(POPULAR_SIZE);

        if (popularIds.isEmpty()) {
            return locationRepository.findByAdminCodes(properties.featuredAdminCodes());
        }

        return locationRepository.findByIds(popularIds);
    }
}
