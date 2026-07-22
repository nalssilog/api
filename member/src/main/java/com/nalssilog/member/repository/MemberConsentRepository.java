package com.nalssilog.member.repository;

import com.nalssilog.member.domain.MemberConsent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 서비스 호출용 MemberConsent 저장소 래퍼.
 */
@Repository
@RequiredArgsConstructor
public class MemberConsentRepository {

    private final MemberConsentJpaRepository memberConsentJpaRepository;

    public void saveAll(List<MemberConsent> consents) {
        memberConsentJpaRepository.saveAll(consents);
    }
}
