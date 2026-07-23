package com.nalssilog.report.application.dto;

/**
 * report 가 location 에서 받아오는 지역 요약(위경도 제외). 제보 응답의 location 필드로 나간다.
 */
public record LocationSummary(
        Long id,
        String sido,
        String sigungu,
        String dong,
        String label,
        String shortLabel
) {
}
