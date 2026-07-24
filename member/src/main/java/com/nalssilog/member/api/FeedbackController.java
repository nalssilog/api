package com.nalssilog.member.api;

import com.nalssilog.member.api.dto.CreateFeedbackRequest;
import com.nalssilog.member.api.dto.FeedbackResponse;
import com.nalssilog.member.application.FeedbackService;
import com.nalssilog.member.config.TrustedProxyClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final TrustedProxyClientIpResolver clientIpResolver;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(@AuthenticationPrincipal Long memberId,
                                   HttpServletRequest httpRequest,
                                   @Valid @RequestBody CreateFeedbackRequest request) {
        return FeedbackResponse.from(
                feedbackService.submit(memberId, clientIpResolver.resolve(httpRequest), request.content()));
    }
}
