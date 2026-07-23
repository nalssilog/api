package com.nalssilog.location.repository;

import com.nalssilog.location.domain.LocationFavorite;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA 인터페이스. 서비스가 직접 호출하지 않고 {@link LocationFavoriteRepository} 래퍼를 통해 사용한다.
 */
public interface LocationFavoriteJpaRepository extends JpaRepository<LocationFavorite, Long> {

    boolean existsByMemberIdAndLocationId(Long memberId, Long locationId);

    long deleteByMemberIdAndLocationId(Long memberId, Long locationId);

    List<LocationFavorite> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 즐겨찾기 많은 지역 id (임시 인기 기준). 나중에 제보 기반으로 교체 예정.
     */
    @Query("select f.locationId from LocationFavorite f group by f.locationId order by count(f) desc")
    List<Long> findPopularLocationIds(Pageable pageable);
}
