package com.nalssilog.report.application.dto;

import java.time.Instant;

/**
 * 한 계산 구간에서 지역별로 집계한 인기 순위 원본 지표.
 */
public record PopularLocationAggregate(
        Long locationId,
        long uniqueReporterCount,
        long reportCount,
        Instant latestReportAt
) {
}
