package com.nalssilog.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.member.domain.MemberConsent;
import com.nalssilog.member.domain.MemberErrorCode;
import com.nalssilog.member.domain.TermsType;
import com.nalssilog.member.repository.MemberConsentRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConsentServiceTest {

    private final MemberConsentRepository repository = mock(MemberConsentRepository.class);
    private final ConsentService service = new ConsentService(repository);

    @Test
    void acceptsAndRecordsBothRequiredTerms() {
        service.recordOnboardingConsents(1L, List.of(
                new TermsAgreement(TermsType.SERVICE, "1.0"),
                new TermsAgreement(TermsType.PRIVACY, "1.0")
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MemberConsent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(MemberConsent::getTermsType)
                .containsExactly(TermsType.SERVICE, TermsType.PRIVACY);
    }

    @Test
    void rejectsSignupWhenPrivacyTermsAreMissing() {
        assertThatThrownBy(() -> service.recordOnboardingConsents(1L, List.of(
                new TermsAgreement(TermsType.SERVICE, "1.0")
        )))
                .isInstanceOfSatisfying(NalssiLogException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(MemberErrorCode.TERMS_NOT_AGREED));
    }
}
