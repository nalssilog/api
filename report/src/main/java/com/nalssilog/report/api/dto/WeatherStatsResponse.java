package com.nalssilog.report.api.dto;

import com.nalssilog.report.application.dto.LocationSummary;
import com.nalssilog.report.application.dto.WeatherStatsData;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 지역 날씨 통계 응답. 최근 윈도우 내 제보들의 3축 분포와 대표값(dominant), 제보 수.
 * distribution 은 enum 전체 값을 0 으로 채워 항상 같은 키를 내려준다(프론트 안정 계약).
 * dominant 는 최다 값(동률이면 enum 선언 순서상 앞선 값), 제보가 없으면 null.
 */
public record WeatherStatsResponse(
        Location location,
        long reportCount,
        Axis temperature,
        Axis precipitation,
        Axis sunlight
) {

    public record Location(
            String id,
            String sido,
            String sigungu,
            String dong,
            String label,
            String shortLabel
    ) {
    }

    public record Axis(String dominant, Map<String, Long> distribution) {
    }

    public static WeatherStatsResponse of(LocationSummary location, WeatherStatsData stats) {
        Location locationDto = new Location(
                String.valueOf(location.id()),
                location.sido(),
                location.sigungu(),
                location.dong(),
                location.label(),
                location.shortLabel());

        return new WeatherStatsResponse(
                locationDto,
                stats.reportCount(),
                axis(Temperature.values(), stats.temperature()),
                axis(Precipitation.values(), stats.precipitation()),
                axis(Sunlight.values(), stats.sunlight())
        );
    }

    private static <E extends Enum<E>> Axis axis(E[] values, Map<E, Long> counts) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        String dominant = null;
        long max = 0;

        for (E value : values) {
            long count = counts.getOrDefault(value, 0L);

            distribution.put(value.name(), count);

            if (count > max) {
                max = count;
                dominant = value.name();
            }
        }

        return new Axis(dominant, distribution);
    }
}
