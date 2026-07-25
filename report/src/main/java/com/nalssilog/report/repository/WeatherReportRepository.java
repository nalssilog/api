package com.nalssilog.report.repository;

import static com.nalssilog.report.domain.QWeatherReport.weatherReport;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.application.dto.ReportData;
import com.nalssilog.report.application.dto.WeatherStatsData;
import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import com.nalssilog.report.domain.WeatherReport;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 서비스 호출용 WeatherReport 저장소.
 * 단순 조회는 Spring Data JPA에 위임하고, 복합 조회와 집계는 QueryDSL로 처리한다.
 */
@Repository
@RequiredArgsConstructor
public class WeatherReportRepository {

    private final WeatherReportJpaRepository weatherReportJpaRepository;
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    public ReportData save(WeatherReport report) {
        return ReportData.of(weatherReportJpaRepository.save(report));
    }

    /**
     * 탈퇴 회원의 제보를 익명화한다(삭제 없이 작성자만 ANONYMOUS 로). 반환값은 익명화된 제보 수.
     */
    public int anonymizeAuthor(Long memberId) {
        entityManager.flush();
        long affectedRows = queryFactory
                .update(weatherReport)
                .set(weatherReport.authorType, ActorType.ANONYMOUS)
                .setNull(weatherReport.authorMemberId)
                .set(weatherReport.authorAnonymousKey, "withdrawn-" + memberId)
                .where(
                        weatherReport.authorType.eq(ActorType.MEMBER),
                        weatherReport.authorMemberId.eq(memberId)
                )
                .execute();
        entityManager.clear();

        return Math.toIntExact(affectedRows);
    }

    public ReportData getReport(Long reportId) {
        return ReportData.of(getReportEntity(reportId));
    }

    /** 삭제처럼 관리 엔티티가 필요한 쓰기 유스케이스 전용. 반드시 트랜잭션 안에서 사용한다. */
    public WeatherReport getReportEntity(Long reportId) {
        return weatherReportJpaRepository.findById(reportId)
                .orElseThrow(() -> new NalssiLogException(ReportErrorCode.REPORT_NOT_FOUND));
    }

    public void delete(WeatherReport report) {
        weatherReportJpaRepository.delete(report);
    }

    public List<ReportData> findPage(Long locationId, Instant cursorTime, Long cursorId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<WeatherReport> reports = cursorTime == null
                ? weatherReportJpaRepository.findAllByLocationIdOrderByCreatedAtDescIdDesc(locationId, pageable)
                : findAfterLocationCursor(locationId, cursorTime, cursorId, limit);

        return reports.stream()
                .map(ReportData::of)
                .toList();
    }

    public List<ReportData> findMemberPage(Long memberId, Instant cursorTime, Long cursorId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<WeatherReport> reports = cursorTime == null
                ? weatherReportJpaRepository.findAllByAuthorTypeAndAuthorMemberIdOrderByCreatedAtDescIdDesc(
                        ActorType.MEMBER, memberId, pageable)
                : findAfterMemberCursor(memberId, cursorTime, cursorId, limit);

        return reports.stream()
                .map(ReportData::of)
                .toList();
    }

    /** 최근({@code since} 이후) 제보 수가 많은 순으로 상위 locationId 목록(인기 지역 랭킹용). */
    public List<Long> topLocationIds(Instant since, int size) {
        return queryFactory
                .select(weatherReport.locationId)
                .from(weatherReport)
                .where(weatherReport.createdAt.goe(since))
                .groupBy(weatherReport.locationId)
                .orderBy(weatherReport.id.count().desc())
                .limit(size)
                .fetch();
    }

    /** 최근({@code since} 이후) 제보의 3축 분포 + 제보 수 집계. */
    public WeatherStatsData statsSince(Long locationId, Instant since) {
        long reportCount = weatherReportJpaRepository
                .countByLocationIdAndCreatedAtGreaterThanEqual(locationId, since);

        return new WeatherStatsData(
                reportCount,
                countByAxis(weatherReport.temperature, Temperature.class, locationId, since),
                countByAxis(weatherReport.precipitation, Precipitation.class, locationId, since),
                countByAxis(weatherReport.sunlight, Sunlight.class, locationId, since)
        );
    }

    private List<WeatherReport> findAfterLocationCursor(
            Long locationId, Instant cursorTime, Long cursorId, int limit) {
        return queryFactory
                .selectFrom(weatherReport)
                .where(
                        weatherReport.locationId.eq(locationId),
                        beforeCursor(cursorTime, cursorId)
                )
                .orderBy(weatherReport.createdAt.desc(), weatherReport.id.desc())
                .limit(limit)
                .fetch();
    }

    private List<WeatherReport> findAfterMemberCursor(
            Long memberId, Instant cursorTime, Long cursorId, int limit) {
        return queryFactory
                .selectFrom(weatherReport)
                .where(
                        weatherReport.authorType.eq(ActorType.MEMBER),
                        weatherReport.authorMemberId.eq(memberId),
                        beforeCursor(cursorTime, cursorId)
                )
                .orderBy(weatherReport.createdAt.desc(), weatherReport.id.desc())
                .limit(limit)
                .fetch();
    }

    private BooleanExpression beforeCursor(Instant cursorTime, Long cursorId) {
        return weatherReport.createdAt.lt(cursorTime)
                .or(weatherReport.createdAt.eq(cursorTime).and(weatherReport.id.lt(cursorId)));
    }

    private <E extends Enum<E>> Map<E, Long> countByAxis(
            EnumPath<E> axis, Class<E> type, Long locationId, Instant since) {
        NumberExpression<Long> count = weatherReport.id.count();
        List<Tuple> rows = queryFactory
                .select(axis, count)
                .from(weatherReport)
                .where(
                        weatherReport.locationId.eq(locationId),
                        weatherReport.createdAt.goe(since)
                )
                .groupBy(axis)
                .fetch();

        Map<E, Long> counts = new EnumMap<>(type);
        rows.forEach(row -> counts.put(row.get(axis), row.get(count)));

        return counts;
    }
}
