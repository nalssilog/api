package com.nalssilog.location.repository;

import com.nalssilog.location.domain.Location;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 단순 CRUD와 메서드 이름으로 표현 가능한 조회만 담당한다.
 * 키워드 검색과 원자적 등록은 {@link LocationRepository}가 담당한다.
 */
public interface LocationJpaRepository extends JpaRepository<Location, Long> {

    List<Location> findByAdminCodeIn(List<String> adminCodes);

    Optional<Location> findByAdminCode(String adminCode);
}
