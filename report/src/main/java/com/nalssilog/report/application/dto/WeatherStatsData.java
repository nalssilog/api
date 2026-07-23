package com.nalssilog.report.application.dto;

import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import java.util.Map;

/**
 * 저장소가 집계한 지역 날씨 통계 원본(지역 label enrich 전). 최근 윈도우 내 3축 분포 + 제보 수.
 */
public record WeatherStatsData(
        long reportCount,
        Map<Temperature, Long> temperature,
        Map<Precipitation, Long> precipitation,
        Map<Sunlight, Long> sunlight
) {
}
