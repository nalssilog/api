package com.nalssilog.report.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.member.domain.TermsType;
import com.nalssilog.report.domain.ReportErrorCode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 비회원 제보에 필요한 필수 약관 동의를 검증하고 저장 가능한 형태로 정규화한다.
 */
@Component
public class ReportConsentPolicy {

    private static final int MAX_VERSION_LENGTH = 20;
    private static final List<TermsType> REQUIRED_TYPES = List.of(
            TermsType.SERVICE,
            TermsType.PRIVACY
    );

    public List<TermsAgreement> validate(List<TermsAgreement> agreedTerms) {
        if (agreedTerms == null) {
            throw termsNotAgreed();
        }

        Map<TermsType, String> versions = new EnumMap<>(TermsType.class);

        for (TermsAgreement agreement : agreedTerms) {
            if (agreement == null || agreement.type() == null || !REQUIRED_TYPES.contains(agreement.type())) {
                throw termsNotAgreed();
            }

            String version = normalizeVersion(agreement.version());
            String existing = versions.putIfAbsent(agreement.type(), version);

            if (existing != null) {
                throw termsNotAgreed();
            }
        }

        if (!versions.keySet().containsAll(REQUIRED_TYPES)) {
            throw termsNotAgreed();
        }

        List<TermsAgreement> normalized = new ArrayList<>(REQUIRED_TYPES.size());

        REQUIRED_TYPES.forEach(type -> normalized.add(new TermsAgreement(type, versions.get(type))));

        return List.copyOf(normalized);
    }

    private String normalizeVersion(String version) {
        if (version == null) {
            throw termsNotAgreed();
        }

        String normalized = version.strip();

        if (normalized.isEmpty() || normalized.length() > MAX_VERSION_LENGTH) {
            throw termsNotAgreed();
        }

        return normalized;
    }

    private NalssiLogException termsNotAgreed() {
        return new NalssiLogException(ReportErrorCode.TERMS_NOT_AGREED);
    }
}
