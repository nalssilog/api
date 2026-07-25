package com.nalssilog.report.repository;

import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.Thanks;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 단순 CRUD와 메서드 이름으로 표현 가능한 조회만 담당한다.
 * 배치 집계와 복합 조건 조회는 {@link ThanksRepository}가 QueryDSL로 처리한다.
 */
public interface ThanksJpaRepository extends JpaRepository<Thanks, Long> {

    boolean existsByReportIdAndActorTypeAndActorKey(Long reportId, ActorType actorType, String actorKey);

    long deleteByReportIdAndActorTypeAndActorKey(Long reportId, ActorType actorType, String actorKey);

    long deleteByReportId(Long reportId);

    long countByReportId(Long reportId);
}
