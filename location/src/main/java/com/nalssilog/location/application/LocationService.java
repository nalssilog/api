package com.nalssilog.location.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.client.KakaoMapClient;
import com.nalssilog.location.client.KakaoRegion;
import com.nalssilog.location.config.LocationProperties;
import com.nalssilog.location.domain.LocationErrorCode;
import com.nalssilog.location.repository.LocationRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지역 검색·조회·역지오코딩·인기 지역 유스케이스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private static final int POPULAR_SIZE = 5;
    private static final String FORMER_JEONBUK = "전라북도";
    private static final String CURRENT_JEONBUK = "전북특별자치도";
    private static final String FORMER_JEONNAM = "전라남도";
    private static final String FORMER_GWANGJU = "광주광역시";
    private static final String CURRENT_JEONNAM_GWANGJU = "전남광주통합특별시";

    private final LocationRepository locationRepository;
    private final PopularLocationSource popularLocationSource;
    private final LocationProperties properties;
    private final KakaoMapClient kakaoMapClient;

    public List<LocationInfo> search(String keyword) {
        return locationRepository.searchByKeyword(normalizeLegacyRegionName(keyword.strip()));
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

    /** GPS 좌표를 카카오맵 법정동으로 변환하고, 서비스에서 사용할 Location 을 조회하거나 등록한다. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LocationInfo reverseGeocode(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        KakaoRegion region = kakaoMapClient.reverseGeocode(latitude, longitude);

        return locationRepository.findOrCreate(region);
    }

    /** 인기 동네 top5. 최근 제보 지역을 우선하고 부족한 자리는 설정된 대표 지역으로 중복 없이 채운다. */
    public List<LocationInfo> getPopular() {
        List<Long> popularIds = popularLocationSource.topLocationIds(POPULAR_SIZE);
        List<LocationInfo> result = new ArrayList<>(locationRepository.findByIds(popularIds));

        if (result.size() >= POPULAR_SIZE) {
            return List.copyOf(result.subList(0, POPULAR_SIZE));
        }

        Set<Long> addedIds = new HashSet<>();
        result.forEach(location -> addedIds.add(location.id()));

        for (LocationInfo featured : locationRepository.findByAdminCodes(properties.featuredAdminCodes())) {
            if (addedIds.add(featured.id())) {
                result.add(featured);
            }
            if (result.size() == POPULAR_SIZE) {
                break;
            }
        }

        return List.copyOf(result);
    }

    private static void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new NalssiLogException(LocationErrorCode.INVALID_COORDINATES);
        }
    }

    private static String normalizeLegacyRegionName(String keyword) {
        return keyword
                .replace(FORMER_JEONBUK, CURRENT_JEONBUK)
                .replace(FORMER_JEONNAM, CURRENT_JEONNAM_GWANGJU)
                .replace(FORMER_GWANGJU, CURRENT_JEONNAM_GWANGJU);
    }
}
