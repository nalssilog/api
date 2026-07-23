package com.nalssilog.report.repository;

import com.nalssilog.report.application.dto.ThanksCountRow;
import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.Thanks;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA 인터페이스. 서비스가 직접 호출하지 않고 {@link ThanksRepository} 래퍼를 통해 사용한다.
 */
public interface ThanksJpaRepository extends JpaRepository<Thanks, Long> {

    boolean existsByReportIdAndActorTypeAndActorKey(Long reportId, ActorType actorType, String actorKey);

    long deleteByReportIdAndActorTypeAndActorKey(Long reportId, ActorType actorType, String actorKey);

    long deleteByReportId(Long reportId);

    long countByReportId(Long reportId);

    @Query("""
            select new com.nalssilog.report.application.dto.ThanksCountRow(t.reportId, count(t))
            from Thanks t
            where t.reportId in :reportIds
            group by t.reportId
            """)
    List<ThanksCountRow> countByReportIds(@Param("reportIds") Collection<Long> reportIds);

    @Query("""
            select distinct t.reportId from Thanks t
            where t.reportId in :reportIds and t.actorType = :actorType and t.actorKey = :actorKey
            """)
    List<Long> findThankedReportIds(@Param("reportIds") Collection<Long> reportIds,
                                    @Param("actorType") ActorType actorType,
                                    @Param("actorKey") String actorKey);
}
