package com.nalssilog.location.repository;

import static com.nalssilog.location.domain.QLocationFavorite.locationFavorite;

import com.nalssilog.location.domain.LocationFavorite;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 서비스 호출용 LocationFavorite 저장소.
 * 단순 조회는 Spring Data JPA에 위임하고, 인기 지역 집계는 QueryDSL로 처리한다.
 */
@Repository
@RequiredArgsConstructor
public class LocationFavoriteRepository {

    private final LocationFavoriteJpaRepository locationFavoriteJpaRepository;
    private final JPAQueryFactory queryFactory;

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
        return queryFactory
                .select(locationFavorite.locationId)
                .from(locationFavorite)
                .groupBy(locationFavorite.locationId)
                .orderBy(locationFavorite.id.count().desc())
                .limit(size)
                .fetch();
    }
}
