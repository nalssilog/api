package com.nalssilog.member.application;

import com.nalssilog.member.application.dto.FeedbackInfo;
import com.nalssilog.member.domain.Feedback;
import com.nalssilog.member.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서비스 피드백 접수 유스케이스. 로그인 회원(memberId)·비로그인(null) 모두 허용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public FeedbackInfo submit(Long memberId, String content) {
        Feedback feedback = Feedback.create(memberId, content.strip());

        return feedbackRepository.save(feedback);
    }
}
