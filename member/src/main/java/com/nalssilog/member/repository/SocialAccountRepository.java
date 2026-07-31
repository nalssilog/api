package com.nalssilog.member.repository;

import static com.nalssilog.member.domain.QMember.member;
import static com.nalssilog.member.domain.QSocialAccount.socialAccount;

import com.nalssilog.member.application.dto.SocialAccountInfo;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 서비스 호출용 SocialAccount 저장소 래퍼.
 * 조회는 DTO 로, 로그인·연동 같은 쓰기 유스케이스는 관리 엔티티로 다룬다(트랜잭션 내부 전용).
 */
@Repository
@RequiredArgsConstructor
public class SocialAccountRepository {

    private final SocialAccountJpaRepository socialAccountJpaRepository;
    private final JPAQueryFactory queryFactory;

    // ===== 읽기 =====

    public List<SocialAccountInfo> findInfosByMemberId(Long memberId) {
        return socialAccountJpaRepository.findAllByMemberId(memberId).stream()
                .map(SocialAccountInfo::of)
                .toList();
    }

    public long countByMemberId(Long memberId) {
        return socialAccountJpaRepository.countByMemberId(memberId);
    }

    // ===== 쓰기(관리 엔티티) =====

    public Optional<SocialAccount> findByProviderAndProviderUserId(Provider provider, String providerUserId) {
        return Optional.ofNullable(queryFactory
                .selectFrom(socialAccount)
                .join(socialAccount.member, member)
                .fetchJoin()
                .where(
                        socialAccount.provider.eq(provider),
                        socialAccount.providerUserId.eq(providerUserId))
                .fetchOne());
    }

    public Optional<SocialAccount> findByMemberIdAndProvider(Long memberId, Provider provider) {
        return socialAccountJpaRepository.findByMemberIdAndProvider(memberId, provider);
    }

    public SocialAccount save(SocialAccount socialAccount) {
        return socialAccountJpaRepository.save(socialAccount);
    }

    public SocialAccount saveAndFlush(SocialAccount socialAccount) {
        return socialAccountJpaRepository.saveAndFlush(socialAccount);
    }

    public void delete(SocialAccount socialAccount) {
        socialAccountJpaRepository.delete(socialAccount);
    }

    public void deleteAllByMemberId(Long memberId) {
        socialAccountJpaRepository.deleteByMemberId(memberId);
    }
}
