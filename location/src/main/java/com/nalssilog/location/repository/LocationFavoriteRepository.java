package com.nalssilog.location.repository;

import com.nalssilog.location.domain.LocationFavorite;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/**
 * 서비스 호출용 LocationFavorite 저장소 래퍼.
 */
@Repository
@RequiredArgsConstructor
public class LocationFavoriteRepository {

    private final LocationFavoriteJpaRepository locationFavoriteJpaRepository;

    public boolean exists(Long memberId, Long locationId) {
        return locationFavoriteJpaRepository.existsByMemberIdAndLocationId(memberId, locationId);
    }

    public void save(LocationFavorite favorite) {
        locationFavoriteJpaRepository.save(favorite);
    }

    public void delete(Long memberId, Long locationId) {
        locationFavoriteJpaRepository.deleteByMemberIdAndLocationId(memberId, locationId);
    }

    public List<Long> findFavoriteLocationIds(Long memberId) {
        return locationFavoriteJpaRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(LocationFavorite::getLocationId)
                .toList();
    }

    public List<Long> findPopularLocationIds(int size) {
        return locationFavoriteJpaRepository.findPopularLocationIds(PageRequest.of(0, size));
    }
}
