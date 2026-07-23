package com.nalssilog.member.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.member.domain.MemberConsent;
import com.nalssilog.member.domain.MemberErrorCode;
import com.nalssilog.member.domain.TermsType;
import com.nalssilog.member.repository.MemberConsentRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 약관 동의 기록. 백엔드는 어떤 종류가 필수인지만 검증하고, 동의한 종류·버전을 기록한다.
 */
@Service
@RequiredArgsConstructor
public class ConsentService {

    private final MemberConsentRepository memberConsentRepository;

    @Transactional
    public void recordOnboardingConsents(Long memberId, List<TermsAgreement> agreedTerms) {
        List<TermsAgreement> agreements = agreedTerms == null ? List.of() : agreedTerms;
        Set<TermsType> agreedTypes = agreements.stream()
                .map(TermsAgreement::type)
                .collect(Collectors.toSet());

        if (!agreedTypes.containsAll(TermsType.requiredTypes())) {
            throw new NalssiLogException(MemberErrorCode.TERMS_NOT_AGREED);
        }

        List<MemberConsent> consents = agreements.stream()
                .map(agreement -> MemberConsent.agree(memberId, agreement.type(), agreement.version()))
                .toList();

        memberConsentRepository.saveAll(consents);
    }
}
