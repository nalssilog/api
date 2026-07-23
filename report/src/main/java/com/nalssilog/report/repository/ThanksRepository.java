package com.nalssilog.report.repository;

import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.application.dto.ThanksCountRow;
import com.nalssilog.report.domain.Thanks;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 서비스 호출용 Thanks 저장소 래퍼. 카운트 컬럼 없이 행으로 관리하고, 목록은 GROUP BY 배치로 집계한다.
 */
@Repository
@RequiredArgsConstructor
public class ThanksRepository {

    private final ThanksJpaRepository thanksJpaRepository;

    public void add(Long reportId, ReportActor actor) {
        if (!thanksJpaRepository.existsByReportIdAndActorTypeAndActorKey(reportId, actor.type(), actor.actorKey())) {
            thanksJpaRepository.save(Thanks.create(reportId, actor.type(), actor.actorKey()));
        }
    }

    public void remove(Long reportId, ReportActor actor) {
        thanksJpaRepository.deleteByReportIdAndActorTypeAndActorKey(reportId, actor.type(), actor.actorKey());
    }

    public long count(Long reportId) {
        return thanksJpaRepository.countByReportId(reportId);
    }

    public boolean isThanked(Long reportId, ReportActor actor) {
        return thanksJpaRepository.existsByReportIdAndActorTypeAndActorKey(reportId, actor.type(), actor.actorKey());
    }

    public Map<Long, Long> countByReportIds(Collection<Long> reportIds) {
        if (reportIds.isEmpty()) {
            return Map.of();
        }

        return thanksJpaRepository.countByReportIds(reportIds).stream()
                .collect(Collectors.toMap(ThanksCountRow::reportId, ThanksCountRow::count));
    }

    public Set<Long> thankedReportIds(Collection<Long> reportIds, ReportActor actor) {
        if (reportIds.isEmpty() || actor == null) {
            return Set.of();
        }

        return Set.copyOf(thanksJpaRepository.findThankedReportIds(reportIds, actor.type(), actor.actorKey()));
    }
}
