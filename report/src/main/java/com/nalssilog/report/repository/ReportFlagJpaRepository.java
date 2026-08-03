package com.nalssilog.report.repository;

import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.ReportFlag;
import com.nalssilog.report.domain.ReportFlagStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportFlagJpaRepository extends JpaRepository<ReportFlag, Long> {

    boolean existsByReport_IdAndReporterTypeAndReporterKey(
            Long reportId, ActorType reporterType, String reporterKey);

    @EntityGraph(attributePaths = "report")
    Page<ReportFlag> findAllByOrderByCreatedAtAsc(Pageable pageable);

    @EntityGraph(attributePaths = "report")
    Page<ReportFlag> findAllByStatusOrderByCreatedAtAsc(ReportFlagStatus status, Pageable pageable);

    List<ReportFlag> findAllByReport_IdAndStatus(Long reportId, ReportFlagStatus status);
}
