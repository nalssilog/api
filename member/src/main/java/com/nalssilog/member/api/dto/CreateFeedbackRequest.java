package com.nalssilog.member.api.dto;

import com.nalssilog.member.domain.Feedback;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 서비스 피드백 제출 요청. 내용만 받는다(최대 500자). */
public record CreateFeedbackRequest(
        @NotBlank @Size(max = Feedback.CONTENT_MAX_LENGTH) String content
) {
}
