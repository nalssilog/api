package com.nalssilog.report.repository;

import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.WeatherReport;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 단순 CRUD와 메서드 이름으로 표현 가능한 조회만 담당한다.
 * 커서 조건, 집계, 벌크 변경은 {@link WeatherReportRepository}가 QueryDSL로 처리한다.
 */
public interface WeatherReportJpaRepository extends JpaRepository<WeatherReport, Long> {

    List<WeatherReport> findAllByLocationIdOrderByCreatedAtDescIdDesc(Long locationId, Pageable pageable);

    List<WeatherReport> findAllByAuthorTypeAndAuthorMemberIdOrderByCreatedAtDescIdDesc(
            ActorType authorType, Long authorMemberId, Pageable pageable);

    long countByLocationIdAndCreatedAtGreaterThanEqual(Long locationId, Instant since);
}
