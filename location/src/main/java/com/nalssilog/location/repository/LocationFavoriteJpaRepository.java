package com.nalssilog.location.repository;

import com.nalssilog.location.domain.LocationFavorite;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 단순 CRUD와 메서드 이름으로 표현 가능한 조회만 담당한다.
 * 인기 지역 집계는 {@link LocationFavoriteRepository}가 QueryDSL로 처리한다.
 */
public interface LocationFavoriteJpaRepository extends JpaRepository<LocationFavorite, Long> {

    boolean existsByMemberIdAndLocationId(Long memberId, Long locationId);

    long deleteByMemberIdAndLocationId(Long memberId, Long locationId);

    List<LocationFavorite> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);
}
