package com.nalssilog.location.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.common.response.PageResponse;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.application.dto.PopularLocationSnapshotData;
import com.nalssilog.location.application.dto.PopularLocationSnapshotInfo;
import com.nalssilog.location.client.KakaoMapClient;
import com.nalssilog.location.client.KakaoRegion;
import com.nalssilog.location.domain.LocationErrorCode;
import com.nalssilog.location.repository.LocationRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private static final int PAGE_SIZE = 5;
    private static final String FORMER_JEONBUK = "전라북도";
    private static final String CURRENT_JEONBUK = "전북특별자치도";
    private static final String FORMER_JEONNAM = "전라남도";
    private static final String FORMER_GWANGJU = "광주광역시";
    private static final String CURRENT_JEONNAM_GWANGJU = "전남광주통합특별시";
    private static final String FORMER_JEOLLA = "전라도";
    private static final String FORMER_JEOLLA_SHORT = "전라";
    private static final String CURRENT_JEOLLA_PREFIX = "전";

    private final LocationRepository locationRepository;
    private final PopularLocationSource popularLocationSource;
    private final KakaoMapClient kakaoMapClient;

    public PageResponse<LocationInfo> search(String keyword, int page) {
        String normalizedKeyword = normalizeLegacyRegionName(
                keyword.strip().replaceAll("\\s+", " "));
        Page<LocationInfo> result = locationRepository.searchByKeyword(
                normalizedKeyword,
                PageRequest.of(page, PAGE_SIZE));

        return PageResponse.from(result);
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

    public PopularLocationSnapshotInfo getPopular() {
        PopularLocationSnapshotData snapshot = popularLocationSource.latestSnapshot();
        List<Long> locationIds = snapshot.rankings().stream()
                .map(PopularLocationSnapshotData.Rank::locationId)
                .toList();
        Map<Long, LocationInfo> locationsById = locationRepository.findByIds(locationIds).stream()
                .collect(Collectors.toMap(LocationInfo::id, Function.identity()));
        List<PopularLocationSnapshotInfo.Item> items = snapshot.rankings().stream()
                .map(rank -> popularItem(rank, locationsById))
                .toList();

        return new PopularLocationSnapshotInfo(
                snapshot.snapshotId(),
                snapshot.calculatedAt(),
                snapshot.windowStartedAt(),
                snapshot.windowEndedAt(),
                snapshot.algorithmVersion(),
                items);
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
                .replace(FORMER_GWANGJU, CURRENT_JEONNAM_GWANGJU)
                .replace(FORMER_JEOLLA, CURRENT_JEOLLA_PREFIX)
                .replace(FORMER_JEOLLA_SHORT, CURRENT_JEOLLA_PREFIX);
    }

    private static PopularLocationSnapshotInfo.Item popularItem(
            PopularLocationSnapshotData.Rank rank,
            Map<Long, LocationInfo> locationsById
    ) {
        LocationInfo location = locationsById.get(rank.locationId());

        if (location == null) {
            throw new IllegalStateException(
                    "popular snapshot references missing location: " + rank.locationId());
        }

        return new PopularLocationSnapshotInfo.Item(
                rank.rank(),
                rank.previousRank(),
                rank.rankChange(),
                rank.movement(),
                rank.uniqueReporterCount(),
                rank.reportCount(),
                rank.latestReportAt(),
                location);
    }
}
