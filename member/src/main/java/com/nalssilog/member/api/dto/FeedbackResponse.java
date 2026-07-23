package com.nalssilog.member.api.dto;

import com.nalssilog.member.application.dto.FeedbackInfo;
import java.time.Instant;

public record FeedbackResponse(
        String id,
        String content,
        Instant createdAt
) {

    public static FeedbackResponse from(FeedbackInfo feedback) {
        return new FeedbackResponse(
                String.valueOf(feedback.id()),
                feedback.content(),
                feedback.createdAt()
        );
    }
}
