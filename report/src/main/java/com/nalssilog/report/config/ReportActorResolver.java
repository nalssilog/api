package com.nalssilog.report.config;

import com.nalssilog.report.application.dto.ReportActor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 요청 주체를 회원(principal) 또는 익명(쿠키)으로 해석한다.
 * 쓰기(작성·감사)는 익명 쿠키를 없으면 발급, 읽기(isThanked)는 있으면 쓰고 없으면 주체 없음(null).
 */
@Component
@RequiredArgsConstructor
public class ReportActorResolver {

    private final AnonymousIdManager anonymousIdManager;

    public ReportActor resolveForWrite(Long memberId, HttpServletRequest request, HttpServletResponse response) {
        if (memberId != null) {
            return ReportActor.member(memberId);
        }

        return ReportActor.anonymous(anonymousIdManager.getOrIssue(request, response));
    }

    public ReportActor resolveForRead(Long memberId, HttpServletRequest request) {
        if (memberId != null) {
            return ReportActor.member(memberId);
        }

        return anonymousIdManager.read(request)
                .map(ReportActor::anonymous)
                .orElse(null);
    }
}
