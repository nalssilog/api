package com.nalssilog.location.repository;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.client.KakaoRegion;
import com.nalssilog.location.domain.Location;
import com.nalssilog.location.domain.LocationErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서비스 호출용 Location 저장소 래퍼. 조회는 DTO 로 반환한다.
 */
@Repository
@RequiredArgsConstructor
public class LocationRepository {

    private final LocationJpaRepository locationJpaRepository;

    private static final int SEARCH_LIMIT = 20;

    public List<LocationInfo> searchByKeyword(String keyword) {
        return locationJpaRepository.searchByKeyword(keyword, PageRequest.of(0, SEARCH_LIMIT)).stream()
                .map(LocationInfo::of)
                .toList();
    }

    public LocationInfo getById(Long id) {
        return locationJpaRepository.findById(id)
                .map(LocationInfo::of)
                .orElseThrow(() -> new NalssiLogException(LocationErrorCode.LOCATION_NOT_FOUND));
    }

    /**
     * 주어진 id 순서를 보존해 조회한다. (인기·즐겨찾기 목록의 정렬 유지용)
     */
    public List<LocationInfo> findByIds(List<Long> ids) {
        Map<Long, Location> byId = locationJpaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Location::getId, Function.identity()));

        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(LocationInfo::of)
                .toList();
    }

    /** 대표 지역을 admin_code 순서대로 조회한다(설정에 있으나 DB 에 없는 코드는 조용히 제외). */
    public List<LocationInfo> findByAdminCodes(List<String> adminCodes) {
        Map<String, Location> byCode = locationJpaRepository.findByAdminCodeIn(adminCodes).stream()
                .collect(Collectors.toMap(Location::getAdminCode, Function.identity()));

        return adminCodes.stream()
                .map(byCode::get)
                .filter(Objects::nonNull)
                .map(LocationInfo::of)
                .toList();
    }

    public boolean isEmpty() {
        return locationJpaRepository.count() == 0;
    }

    public void saveAll(List<Location> locations) {
        locationJpaRepository.saveAll(locations);
    }

    /**
     * 카카오 법정동 코드로 지역을 원자적으로 등록한 뒤 반환한다.
     * 같은 법정동에 요청이 동시에 들어와도 DB unique + ON CONFLICT 로 한 행만 유지한다.
     */
    @Transactional
    public LocationInfo findOrCreate(KakaoRegion region) {
        locationJpaRepository.insertIfAbsent(
                region.adminCode(),
                region.sido(),
                region.sigungu(),
                region.dong(),
                region.latitude(),
                region.longitude()
        );

        return locationJpaRepository.findByAdminCode(region.adminCode())
                .map(LocationInfo::of)
                .orElseThrow(() -> new NalssiLogException(LocationErrorCode.LOCATION_NOT_FOUND));
    }
}
