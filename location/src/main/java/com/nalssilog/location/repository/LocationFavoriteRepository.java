package com.nalssilog.location.repository;

import com.nalssilog.location.domain.LocationFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 단순 CRUD와 메서드 이름으로 표현 가능한 조회만 담당한다.
 */
public interface LocationFavoriteRepository extends JpaRepository<LocationFavorite, Long> {

    boolean existsByMemberIdAndLocationId(Long memberId, Long locationId);

    long deleteByMemberIdAndLocationId(Long memberId, Long locationId);

    Page<LocationFavorite> findAllByMemberIdOrderByCreatedAtDescIdDesc(Long memberId, Pageable pageable);
}
