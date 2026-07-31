package com.nalssilog.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.member.application.dto.FeedbackInfo;
import com.nalssilog.member.domain.Feedback;
import com.nalssilog.member.repository.FeedbackRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("java:S5960")
class FeedbackServiceTest {

    private final FeedbackRepository repository = mock(FeedbackRepository.class);
    private final FeedbackRateLimiter rateLimiter = mock(FeedbackRateLimiter.class);
    private final FeedbackService service = new FeedbackService(repository, rateLimiter);

    @Test
    void anonymousFeedbackIsRateCheckedAndStoredWithoutMemberId() {
        FeedbackInfo saved = new FeedbackInfo(1L, null, "좋아요", Instant.now());

        when(repository.save(org.mockito.ArgumentMatchers.any(Feedback.class))).thenReturn(saved);

        FeedbackInfo result = service.submit(null, "client-a.test", "  좋아요  ");

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);

        verify(rateLimiter).check(null, "client-a.test");
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAuthorMemberId()).isNull();
        assertThat(captor.getValue().getContent()).isEqualTo("좋아요");
        assertThat(result).isSameAs(saved);
    }
}
