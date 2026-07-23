package com.nalssilog.member.application.dto;

import com.nalssilog.member.domain.Feedback;
import java.time.Instant;

/**
 * 접수된 피드백 정보 계약.
 */
public record FeedbackInfo(
        Long id,
        Long authorMemberId,
        String content,
        Instant createdAt
) {

    public static FeedbackInfo of(Feedback feedback) {
        return new FeedbackInfo(
                feedback.getId(),
                feedback.getAuthorMemberId(),
                feedback.getContent(),
                feedback.getCreatedAt()
        );
    }
}
