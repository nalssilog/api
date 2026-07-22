package com.nalssilog.report.application;

import com.nalssilog.member.domain.event.MemberWithdrawnEvent;
import com.nalssilog.report.repository.WeatherReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회원 탈퇴 이벤트를 구독해 해당 회원의 제보를 익명화한다.
 * member 트랜잭션 커밋 이후(AFTER_COMMIT)에 별도 트랜잭션으로 실행한다.
 * (지금은 모듈 내부 구독, MSA 분리 시 브로커 구독으로 교체)
 */
@Component
@RequiredArgsConstructor
public class MemberWithdrawalListener {

    private final WeatherReportRepository reportRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberWithdrawn(MemberWithdrawnEvent event) {
        reportRepository.anonymizeAuthor(event.memberId());
    }
}
