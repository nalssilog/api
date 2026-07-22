package com.nalssilog.report.application.dto;

/**
 * GROUP BY 배치 집계 결과 한 행. (JPQL 생성자 표현식 대상)
 */
public record ThanksCountRow(Long reportId, Long count) {
}
