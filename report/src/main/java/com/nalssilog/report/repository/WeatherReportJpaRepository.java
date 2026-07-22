package com.nalssilog.report.repository;

import com.nalssilog.report.domain.WeatherReport;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA 인터페이스. 서비스가 직접 호출하지 않고 {@link WeatherReportRepository} 래퍼를 통해 사용한다.
 */
public interface WeatherReportJpaRepository extends JpaRepository<WeatherReport, Long> {

    @Query("""
            select r from WeatherReport r
            where r.locationId = :locationId and r.createdAt >= :since
            order by r.createdAt desc, r.id desc
            """)
    List<WeatherReport> findFirstPage(@Param("locationId") Long locationId,
                                      @Param("since") Instant since,
                                      Pageable pageable);

    @Query("""
            select r from WeatherReport r
            where r.locationId = :locationId and r.createdAt >= :since
              and (r.createdAt < :cursorTime or (r.createdAt = :cursorTime and r.id < :cursorId))
            order by r.createdAt desc, r.id desc
            """)
    List<WeatherReport> findAfterCursor(@Param("locationId") Long locationId,
                                        @Param("since") Instant since,
                                        @Param("cursorTime") Instant cursorTime,
                                        @Param("cursorId") Long cursorId,
                                        Pageable pageable);

    @Query("""
            select r from WeatherReport r
            where r.authorType = com.nalssilog.report.domain.ActorType.MEMBER and r.authorMemberId = :memberId
            order by r.createdAt desc, r.id desc
            """)
    List<WeatherReport> findFirstMemberPage(@Param("memberId") Long memberId, Pageable pageable);

    @Query("""
            select r from WeatherReport r
            where r.authorType = com.nalssilog.report.domain.ActorType.MEMBER and r.authorMemberId = :memberId
              and (r.createdAt < :cursorTime or (r.createdAt = :cursorTime and r.id < :cursorId))
            order by r.createdAt desc, r.id desc
            """)
    List<WeatherReport> findMemberAfterCursor(@Param("memberId") Long memberId,
                                              @Param("cursorTime") Instant cursorTime,
                                              @Param("cursorId") Long cursorId,
                                              Pageable pageable);

    /**
     * 탈퇴 회원의 제보를 익명화한다(삭제하지 않음). 작성자를 ANONYMOUS 로 바꾸고 회원 참조를 끊는다.
     * anonymousKey 는 탈퇴 회원마다 고정값(원래 익명 제보와 동일하게 "익명의 이웃"으로 렌더됨).
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            update WeatherReport r
               set r.authorType = com.nalssilog.report.domain.ActorType.ANONYMOUS,
                   r.authorMemberId = null,
                   r.authorAnonymousKey = :anonymousKey
             where r.authorType = com.nalssilog.report.domain.ActorType.MEMBER and r.authorMemberId = :memberId
            """)
    int anonymizeByMemberId(@Param("memberId") Long memberId, @Param("anonymousKey") String anonymousKey);

    long countByLocationIdAndCreatedAtGreaterThanEqual(Long locationId, Instant since);

    @Query("""
            select r.locationId from WeatherReport r
            where r.createdAt >= :since
            group by r.locationId
            order by count(r) desc
            """)
    List<Long> topLocationIdsSince(@Param("since") Instant since, Pageable pageable);

    @Query("""
            select r.temperature, count(r) from WeatherReport r
            where r.locationId = :locationId and r.createdAt >= :since
            group by r.temperature
            """)
    List<Object[]> temperatureCounts(@Param("locationId") Long locationId, @Param("since") Instant since);

    @Query("""
            select r.precipitation, count(r) from WeatherReport r
            where r.locationId = :locationId and r.createdAt >= :since
            group by r.precipitation
            """)
    List<Object[]> precipitationCounts(@Param("locationId") Long locationId, @Param("since") Instant since);

    @Query("""
            select r.sunlight, count(r) from WeatherReport r
            where r.locationId = :locationId and r.createdAt >= :since
            group by r.sunlight
            """)
    List<Object[]> sunlightCounts(@Param("locationId") Long locationId, @Param("since") Instant since);
}
