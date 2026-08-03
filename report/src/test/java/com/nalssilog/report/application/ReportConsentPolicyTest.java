package com.nalssilog.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.member.domain.TermsType;
import com.nalssilog.report.domain.ReportErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportConsentPolicyTest {

    private final ReportConsentPolicy policy = new ReportConsentPolicy();

    @Test
    void acceptsBothRequiredTermsAndNormalizesVersions() {
        List<TermsAgreement> agreements = policy.validate(List.of(
                new TermsAgreement(TermsType.PRIVACY, " privacy-2026-08 "),
                new TermsAgreement(TermsType.SERVICE, " service-1.0 ")
        ));

        assertThat(agreements)
                .extracting(TermsAgreement::type)
                .containsExactly(TermsType.SERVICE, TermsType.PRIVACY);
        assertThat(agreements)
                .extracting(TermsAgreement::version)
                .containsExactly("service-1.0", "privacy-2026-08");
    }

    @Test
    void rejectsWhenPrivacyAgreementIsMissing() {
        assertTermsNotAgreed(List.of(
                new TermsAgreement(TermsType.SERVICE, "1.0")
        ));
    }

    @Test
    void rejectsMissingAgreementList() {
        assertTermsNotAgreed(null);
    }

    @Test
    void rejectsDuplicateAgreementTypes() {
        assertTermsNotAgreed(List.of(
                new TermsAgreement(TermsType.SERVICE, "1.0"),
                new TermsAgreement(TermsType.SERVICE, "1.0"),
                new TermsAgreement(TermsType.PRIVACY, "1.0")
        ));
    }

    @Test
    void rejectsBlankOrOversizedVersions() {
        assertTermsNotAgreed(List.of(
                new TermsAgreement(TermsType.SERVICE, " "),
                new TermsAgreement(TermsType.PRIVACY, "1.0")
        ));
        assertTermsNotAgreed(List.of(
                new TermsAgreement(TermsType.SERVICE, "1.0"),
                new TermsAgreement(TermsType.PRIVACY, "v".repeat(21))
        ));
    }

    private void assertTermsNotAgreed(List<TermsAgreement> agreements) {
        assertThatThrownBy(() -> policy.validate(agreements))
                .isInstanceOfSatisfying(NalssiLogException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ReportErrorCode.TERMS_NOT_AGREED));
    }
}
