package com.nalssilog.report.application.dto;

import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.WeatherReport;

/**
 * 제보 작성·감사해요의 주체. 회원(memberId) 또는 익명(anonymousKey).
 * actorKey 는 Thanks 중복 판단에 쓰는 통일 키(회원=memberId 문자열, 익명=UUID).
 */
public record ReportActor(ActorType type, Long memberId, String anonymousKey) {

    public static ReportActor member(Long memberId) {
        return new ReportActor(ActorType.MEMBER, memberId, null);
    }

    public static ReportActor anonymous(String anonymousKey) {
        return new ReportActor(ActorType.ANONYMOUS, null, anonymousKey);
    }

    public static ReportActor authorOf(WeatherReport report) {
        return report.getAuthorType() == ActorType.MEMBER
                ? member(report.getAuthorMemberId())
                : anonymous(report.getAuthorAnonymousKey());
    }

    public String actorKey() {
        return type == ActorType.MEMBER ? String.valueOf(memberId) : anonymousKey;
    }
}
