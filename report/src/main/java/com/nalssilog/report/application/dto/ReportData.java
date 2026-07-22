package com.nalssilog.report.application.dto;

import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import com.nalssilog.report.domain.WeatherReport;
import com.nalssilog.report.domain.WeatherReportImage;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * 저장소에서 꺼낸 제보 원본 데이터(회원·지역 enrich 전). 서비스가 client 로 작성자·지역을 채워 응답을 만든다.
 */
public record ReportData(
        Long id,
        Long locationId,
        ActorType authorType,
        Long authorMemberId,
        String authorAnonymousKey,
        Temperature temperature,
        Precipitation precipitation,
        Sunlight sunlight,
        String comment,
        List<String> imageKeys,
        Instant createdAt
) {

    public static ReportData of(WeatherReport report) {
        List<String> imageKeys = report.getImages().stream()
                .sorted(Comparator.comparingInt(WeatherReportImage::getDisplayOrder))
                .map(WeatherReportImage::getStorageKey)
                .toList();

        return new ReportData(
                report.getId(),
                report.getLocationId(),
                report.getAuthorType(),
                report.getAuthorMemberId(),
                report.getAuthorAnonymousKey(),
                report.getTemperature(),
                report.getPrecipitation(),
                report.getSunlight(),
                report.getComment(),
                imageKeys,
                report.getCreatedAt()
        );
    }
}
