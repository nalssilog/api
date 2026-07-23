package com.nalssilog.member.repository;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberErrorCode;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.SocialAccount;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 서비스 호출용 Member 저장소 래퍼.
 * 읽기는 DTO({@link MemberInfo})로 변환해 반환하고, 쓰기는 트랜잭션 내부에서 관리 엔티티를 다룬다.
 * Member 애그리거트 읽기 모델(회원 + 소셜 계정) 조립도 이 계층에서 책임진다.
 */
@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final MemberJpaRepository memberJpaRepository;
    private final SocialAccountJpaRepository socialAccountJpaRepository;

    // ===== 읽기: DTO 반환 =====

    public MemberInfo getMemberInfo(Long memberId) {
        Member member = getMember(memberId);

        return toInfo(member);
    }

    public Optional<MemberInfo> findMemberInfo(Long memberId) {
        return memberJpaRepository.findById(memberId)
                .map(this::toInfo);
    }

    public Optional<MemberInfo> findMemberInfoByEmail(String email) {
        return memberJpaRepository.findFirstByEmailAndStatusNot(email, MemberStatus.WITHDRAWN)
                .map(this::toInfo);
    }

    public boolean existsByNickname(String nickname) {
        return memberJpaRepository.existsByNickname(nickname);
    }

    public boolean existsByNicknameExcept(String nickname, Long memberId) {
        return memberJpaRepository.existsByNicknameAndIdNot(nickname, memberId);
    }

    // ===== 쓰기: 관리 엔티티(트랜잭션 내부 전용) =====

    public Member getMember(Long memberId) {
        return memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new NalssiLogException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    public Member save(Member member) {
        return memberJpaRepository.save(member);
    }

    // ===== 매핑 =====

    private MemberInfo toInfo(Member member) {
        List<SocialAccount> accounts = socialAccountJpaRepository.findAllByMemberId(member.getId());

        return MemberInfo.of(member, accounts);
    }
}
