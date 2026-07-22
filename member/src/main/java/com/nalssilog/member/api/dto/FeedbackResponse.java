package com.nalssilog.member.api.dto;

import com.nalssilog.member.application.dto.FeedbackInfo;
import com.nalssilog.member.domain.FeedbackCategory;
import java.time.Instant;

public record FeedbackResponse(
        String id,
        FeedbackCategory category,
        String content,
        Instant createdAt
) {

    public static FeedbackResponse from(FeedbackInfo feedback) {
        return new FeedbackResponse(
                String.valueOf(feedback.id()),
                feedback.category(),
                feedback.content(),
                feedback.createdAt()
        );
    }
}
