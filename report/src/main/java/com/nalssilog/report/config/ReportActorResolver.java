package com.nalssilog.report.config;

import com.nalssilog.common.security.VerifiedRequestCredentials;
import com.nalssilog.report.application.dto.ReportActor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
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

        var mobileGuest = VerifiedRequestCredentials.guestAnonymousKey(request);

        if (mobileGuest.isPresent()) {
            return ReportActor.anonymous(mobileGuest.get());
        }

        return ReportActor.anonymous(anonymousIdManager.getOrIssue(request, response));
    }

    public ReportActor resolveForRead(Long memberId, HttpServletRequest request) {
        if (memberId != null) {
            return ReportActor.member(memberId);
        }

        var mobileGuest = VerifiedRequestCredentials.guestAnonymousKey(request);

        if (mobileGuest.isPresent()) {
            return ReportActor.anonymous(mobileGuest.get());
        }

        return anonymousIdManager.read(request)
                .map(ReportActor::anonymous)
                .orElse(null);
    }

    /**
     * 삭제 소유권 확인용 주체 목록. 로그인 전에 같은 브라우저에서 작성한 익명 제보도 삭제할 수 있게
     * 회원 식별자와 기존 익명 쿠키를 모두 후보로 반환한다. 새 익명 쿠키는 발급하지 않는다.
     */
    public List<ReportActor> resolveForOwnership(Long memberId, HttpServletRequest request) {
        List<ReportActor> actors = new ArrayList<>(2);

        if (memberId != null) {
            actors.add(ReportActor.member(memberId));
        }

        VerifiedRequestCredentials.guestAnonymousKey(request)
                .map(ReportActor::anonymous)
                .ifPresent(actors::add);

        if (!VerifiedRequestCredentials.hasNonCookieCredential(request)) {
            anonymousIdManager.read(request)
                    .map(ReportActor::anonymous)
                    .ifPresent(actors::add);
        }

        return List.copyOf(actors);
    }
}
