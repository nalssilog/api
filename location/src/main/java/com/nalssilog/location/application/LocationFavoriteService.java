package com.nalssilog.location.application;

import com.nalssilog.common.response.PageResponse;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.domain.LocationFavorite;
import com.nalssilog.location.repository.LocationFavoriteRepository;
import com.nalssilog.location.repository.LocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 즐겨찾기 지역 관리.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationFavoriteService {

    private static final int PAGE_SIZE = 5;

    private final LocationFavoriteRepository locationFavoriteRepository;
    private final LocationRepository locationRepository;

    @Transactional
    public void addFavorite(Long memberId, Long locationId) {
        locationRepository.getById(locationId);

        if (!locationFavoriteRepository.existsByMemberIdAndLocationId(memberId, locationId)) {
            locationFavoriteRepository.save(LocationFavorite.of(memberId, locationId));
        }
    }

    @Transactional
    public void removeFavorite(Long memberId, Long locationId) {
        locationFavoriteRepository.deleteByMemberIdAndLocationId(memberId, locationId);
    }

    public PageResponse<LocationInfo> listFavorites(Long memberId, int page) {
        Page<LocationFavorite> favorites =
                locationFavoriteRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(
                        memberId,
                        PageRequest.of(page, PAGE_SIZE));
        List<Long> favoriteIds = favorites.getContent().stream()
                .map(LocationFavorite::getLocationId)
                .toList();

        return PageResponse.of(
                locationRepository.findByIds(favoriteIds),
                page,
                PAGE_SIZE,
                favorites.getTotalElements());
    }
}
