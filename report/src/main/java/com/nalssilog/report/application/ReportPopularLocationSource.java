package com.nalssilog.report.application;

import com.nalssilog.location.application.PopularLocationSource;
import com.nalssilog.report.repository.WeatherReportRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * location 의 인기 지역 port 를 제보 활동 기반으로 구현(adapter). 최근 {@link #POPULAR_WINDOW} 내
 * 제보가 많은 순으로 locationId 를 돌려준다. (report → location 의존이라 location 인터페이스 구현 가능)
 */
@Component
@RequiredArgsConstructor
public class ReportPopularLocationSource implements PopularLocationSource {

    private static final Duration POPULAR_WINDOW = Duration.ofDays(7);

    private final WeatherReportRepository reportRepository;

    @Override
    public List<Long> topLocationIds(int size) {
        return reportRepository.topLocationIds(Instant.now().minus(POPULAR_WINDOW), size);
    }
}
