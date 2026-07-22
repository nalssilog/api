package com.nalssilog.member.repository;

import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 인터페이스. 서비스가 직접 호출하지 않고 {@link SocialAccountRepository} 래퍼를 통해 사용한다.
 */
public interface SocialAccountJpaRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(Provider provider, String providerUserId);

    Optional<SocialAccount> findByMemberIdAndProvider(Long memberId, Provider provider);

    List<SocialAccount> findAllByMemberId(Long memberId);

    long countByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}
