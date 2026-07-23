package com.nalssilog.location.application;

import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.domain.LocationFavorite;
import com.nalssilog.location.repository.LocationFavoriteRepository;
import com.nalssilog.location.repository.LocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 즐겨찾기 지역 관리.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationFavoriteService {

    private final LocationFavoriteRepository locationFavoriteRepository;
    private final LocationRepository locationRepository;

    @Transactional
    public void addFavorite(Long memberId, Long locationId) {
        locationRepository.getById(locationId);

        if (!locationFavoriteRepository.exists(memberId, locationId)) {
            locationFavoriteRepository.save(LocationFavorite.of(memberId, locationId));
        }
    }

    @Transactional
    public void removeFavorite(Long memberId, Long locationId) {
        locationFavoriteRepository.delete(memberId, locationId);
    }

    public List<LocationInfo> listFavorites(Long memberId) {
        List<Long> locationIds = locationFavoriteRepository.findFavoriteLocationIds(memberId);

        return locationRepository.findByIds(locationIds);
    }
}
