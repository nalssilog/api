package com.nalssilog.report.repository;

import static com.nalssilog.report.domain.QThanks.thanks;

import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.domain.Thanks;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 서비스 호출용 Thanks 저장소.
 * 단순 조회는 Spring Data JPA에 위임하고, 배치 집계와 복합 조회는 QueryDSL로 처리한다.
 */
@Repository
@RequiredArgsConstructor
public class ThanksRepository {

    private final ThanksJpaRepository thanksJpaRepository;
    private final JPAQueryFactory queryFactory;

    public void add(Long reportId, ReportActor actor) {
        if (!thanksJpaRepository.existsByReportIdAndActorTypeAndActorKey(reportId, actor.type(), actor.actorKey())) {
            thanksJpaRepository.save(Thanks.create(reportId, actor.type(), actor.actorKey()));
        }
    }

    public void remove(Long reportId, ReportActor actor) {
        thanksJpaRepository.deleteByReportIdAndActorTypeAndActorKey(reportId, actor.type(), actor.actorKey());
    }

    public void deleteAllByReportId(Long reportId) {
        thanksJpaRepository.deleteByReportId(reportId);
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

        NumberExpression<Long> count = thanks.id.count();
        List<Tuple> rows = queryFactory
                .select(thanks.reportId, count)
                .from(thanks)
                .where(thanks.reportId.in(reportIds))
                .groupBy(thanks.reportId)
                .fetch();

        return rows.stream()
                .collect(Collectors.toMap(
                        row -> row.get(thanks.reportId),
                        row -> row.get(count)
                ));
    }

    public Set<Long> thankedReportIds(Collection<Long> reportIds, ReportActor actor) {
        if (reportIds.isEmpty() || actor == null) {
            return Set.of();
        }

        return Set.copyOf(queryFactory
                .select(thanks.reportId)
                .distinct()
                .from(thanks)
                .where(
                        thanks.reportId.in(reportIds),
                        thanks.actorType.eq(actor.type()),
                        thanks.actorKey.eq(actor.actorKey())
                )
                .fetch());
    }
}
