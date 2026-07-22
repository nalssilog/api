package com.nalssilog.report.repository;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.application.dto.ReportData;
import com.nalssilog.report.application.dto.WeatherStatsData;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import com.nalssilog.report.domain.WeatherReport;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 서비스 호출용 WeatherReport 저장소 래퍼. 조회는 ReportData(회원·지역 enrich 전) 로 반환한다.
 */
@Repository
@RequiredArgsConstructor
public class WeatherReportRepository {

    private final WeatherReportJpaRepository weatherReportJpaRepository;

    public ReportData save(WeatherReport report) {
        return ReportData.of(weatherReportJpaRepository.save(report));
    }

    /**
     * 탈퇴 회원의 제보를 익명화한다(삭제 없이 작성자만 ANONYMOUS 로). 반환값은 익명화된 제보 수.
     */
    public int anonymizeAuthor(Long memberId) {
        return weatherReportJpaRepository.anonymizeByMemberId(memberId, "withdrawn-" + memberId);
    }

    public ReportData getReport(Long reportId) {
        return weatherReportJpaRepository.findById(reportId)
                .map(ReportData::of)
                .orElseThrow(() -> new NalssiLogException(ReportErrorCode.REPORT_NOT_FOUND));
    }

    public List<ReportData> findPage(Long locationId, Instant since, Instant cursorTime, Long cursorId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        List<WeatherReport> reports = cursorTime == null
                ? weatherReportJpaRepository.findFirstPage(locationId, since, pageable)
                : weatherReportJpaRepository.findAfterCursor(locationId, since, cursorTime, cursorId, pageable);

        return reports.stream()
                .map(ReportData::of)
                .toList();
    }

    public List<ReportData> findMemberPage(Long memberId, Instant cursorTime, Long cursorId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        List<WeatherReport> reports = cursorTime == null
                ? weatherReportJpaRepository.findFirstMemberPage(memberId, pageable)
                : weatherReportJpaRepository.findMemberAfterCursor(memberId, cursorTime, cursorId, pageable);

        return reports.stream()
                .map(ReportData::of)
                .toList();
    }

    /**
     * 최근({@code since} 이후) 제보 수가 많은 순으로 상위 locationId 목록(인기 지역 랭킹용).
     */
    public List<Long> topLocationIds(Instant since, int size) {
        return weatherReportJpaRepository.topLocationIdsSince(since, PageRequest.of(0, size));
    }

    /**
     * 최근({@code since} 이후) 제보의 3축 분포 + 제보 수 집계.
     */
    public WeatherStatsData statsSince(Long locationId, Instant since) {
        long reportCount = weatherReportJpaRepository
                .countByLocationIdAndCreatedAtGreaterThanEqual(locationId, since);

        return new WeatherStatsData(
                reportCount,
                toEnumMap(weatherReportJpaRepository.temperatureCounts(locationId, since), Temperature.class),
                toEnumMap(weatherReportJpaRepository.precipitationCounts(locationId, since), Precipitation.class),
                toEnumMap(weatherReportJpaRepository.sunlightCounts(locationId, since), Sunlight.class)
        );
    }

    private static <E extends Enum<E>> Map<E, Long> toEnumMap(List<Object[]> rows, Class<E> type) {
        Map<E, Long> counts = new EnumMap<>(type);

        for (Object[] row : rows) {
            counts.put(type.cast(row[0]), (Long) row[1]);
        }

        return counts;
    }
}
