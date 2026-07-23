package com.nalssilog.location.repository;

import com.nalssilog.location.domain.Location;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    List<Location> findByAdminCodeIn(List<String> adminCodes);

    Optional<Location> findByAdminCode(String adminCode);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            insert into location (
                created_at, updated_at, admin_code, sido, sigungu, dong, latitude, longitude
            ) values (
                current_timestamp, current_timestamp, :adminCode, :sido, :sigungu, :dong, :latitude, :longitude
            )
            on conflict (admin_code) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("adminCode") String adminCode,
                       @Param("sido") String sido,
                       @Param("sigungu") String sigungu,
                       @Param("dong") String dong,
                       @Param("latitude") double latitude,
                       @Param("longitude") double longitude);
}
