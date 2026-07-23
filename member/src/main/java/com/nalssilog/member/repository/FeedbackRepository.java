package com.nalssilog.member.repository;

import com.nalssilog.member.application.dto.FeedbackInfo;
import com.nalssilog.member.domain.Feedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 서비스 호출용 Feedback 저장소 래퍼. 저장 후 접수 정보를 DTO 로 반환한다.
 */
@Repository
@RequiredArgsConstructor
public class FeedbackRepository {

    private final FeedbackJpaRepository feedbackJpaRepository;

    public FeedbackInfo save(Feedback feedback) {
        return FeedbackInfo.of(feedbackJpaRepository.save(feedback));
    }
}
