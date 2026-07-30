package com.nalssilog.report.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.nalssilog.common.security.VerifiedRequestCredentials;
import com.nalssilog.report.application.dto.ReportActor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings("java:S5960")
class ReportActorResolverTest {

    private final AnonymousIdManager anonymousIdManager =
            mock(AnonymousIdManager.class);
    private final ReportActorResolver resolver =
            new ReportActorResolver(anonymousIdManager);

    @Test
    void verifiedMobileGuestIsUsedInsteadOfTheWebCookieFallback() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        VerifiedRequestCredentials.markGuest(request, "mobile-anonymous-key");

        ReportActor actor = resolver.resolveForWrite(
                null,
                request,
                new MockHttpServletResponse());

        assertThat(actor)
                .isEqualTo(ReportActor.anonymous("mobile-anonymous-key"));
        verifyNoInteractions(anonymousIdManager);
    }

    @Test
    void authenticatedMemberCanRetainGuestOwnershipCandidate() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        VerifiedRequestCredentials.markGuest(request, "mobile-anonymous-key");

        assertThat(resolver.resolveForOwnership(7L, request))
                .containsExactly(
                        ReportActor.member(7L),
                        ReportActor.anonymous("mobile-anonymous-key"));
        verifyNoInteractions(anonymousIdManager);
    }
}
