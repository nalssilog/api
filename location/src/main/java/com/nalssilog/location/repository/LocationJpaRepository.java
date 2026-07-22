package com.nalssilog.location.repository;

import com.nalssilog.location.domain.Location;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA 인터페이스. 서비스가 직접 호출하지 않고 {@link LocationRepository} 래퍼를 통해 사용한다.
 */
public interface LocationJpaRepository extends JpaRepository<Location, Long> {

    /**
     * 시도/시군구/동, 그리고 "시도 시군구 동" 조합 label 까지 부분검색(대소문자 무시).
     * 예: "강남", "역삼", "서울 강남", "강남구 역삼동" 모두 매칭.
     */
    @Query("""
            select l from Location l
            where lower(l.sido) like lower(concat('%', :keyword, '%'))
               or lower(l.sigungu) like lower(concat('%', :keyword, '%'))
               or lower(l.dong) like lower(concat('%', :keyword, '%'))
               or lower(concat(l.sido, ' ', l.sigungu, ' ', l.dong)) like lower(concat('%', :keyword, '%'))
            order by l.sido asc, l.sigungu asc, l.dong asc
            """)
    List<Location> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 중심좌표 기준 최근접 지역(평면 근사). 카카오 역지오코딩 도입 시 client 로 교체 가능.
     */
    @Query("""
            select l from Location l
            order by (l.latitude - :lat) * (l.latitude - :lat) + (l.longitude - :lng) * (l.longitude - :lng)
            """)
    List<Location> findNearest(@Param("lat") double lat, @Param("lng") double lng, Pageable pageable);

    List<Location> findByAdminCodeIn(List<String> adminCodes);
}
