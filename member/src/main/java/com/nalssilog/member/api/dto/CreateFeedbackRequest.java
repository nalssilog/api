package com.nalssilog.member.api.dto;

import com.nalssilog.member.domain.Feedback;
import com.nalssilog.member.domain.FeedbackCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 서비스 피드백 제출 요청. category 는 선택(미지정 시 OTHER 처리).
 */
public record CreateFeedbackRequest(
        FeedbackCategory category,
        @NotBlank @Size(max = Feedback.CONTENT_MAX_LENGTH) String content
) {
}
