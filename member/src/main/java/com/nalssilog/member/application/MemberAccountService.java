package com.nalssilog.member.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.application.dto.MemberSummary;
import com.nalssilog.member.application.dto.SocialLoginResult;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberErrorCode;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import com.nalssilog.member.domain.event.MemberWithdrawnEvent;
import com.nalssilog.member.repository.MemberRepository;
import com.nalssilog.member.repository.SocialAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAccountService {

	private static final String PROVIDER_USER_CONSTRAINT =
		"uk_social_account_provider_user";
	private static final String MEMBER_PROVIDER_CONSTRAINT =
		"uk_social_account_member_provider";

	private final MemberRepository memberRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final ApplicationEventPublisher eventPublisher;

	private static RuntimeException translateSocialLinkConflict(
		DataIntegrityViolationException exception
	) {
		String constraintName = constraintName(exception);

		if (PROVIDER_USER_CONSTRAINT.equalsIgnoreCase(constraintName)) {
			return new NalssiLogException(MemberErrorCode.SOCIAL_ACCOUNT_IN_USE);
		}
		if (MEMBER_PROVIDER_CONSTRAINT.equalsIgnoreCase(constraintName)) {
			return new NalssiLogException(MemberErrorCode.ACCOUNT_ALREADY_LINKED);
		}

		return exception;
	}

	private static String constraintName(Throwable throwable) {
		for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException constraintViolation) {
				return constraintViolation.getConstraintName();
			}
		}

		return null;
	}

	/** 소셜 인증 결과 분기(생성·병합 안 함). 가입된 소셜=EXISTING, 이메일로 기존 회원 있으면 LINK_REQUIRED, 없으면 NEW. */
	public SocialLoginResult resolveSocialLogin(Provider provider, String providerUserId, String email) {
		Optional<SocialAccount> linked = socialAccountRepository.findByProviderAndProviderUserId(
			provider,
			providerUserId);

		if (linked.isPresent()) {
			SocialAccount account = linked.get();

			return SocialLoginResult.existing(account.getMember().getId(), account.getMember().getStatus());
		}

		if (email == null || email.isBlank()) {
			return SocialLoginResult.newMember(email);
		}

		return memberRepository.findMemberInfoByEmail(email)
			.map(found -> SocialLoginResult.linkRequired(found.id(), email, found.connectedProviders()))
			.orElseGet(() -> SocialLoginResult.newMember(email));
	}

	/** 기존 회원에 새 소셜 계정 연동(호출 전 재인증으로 소유권 증명 전제). */
	@Transactional
	public MemberInfo linkSocial(
		Long targetMemberId,
		Provider provider,
		String providerUserId,
		String email
	) {
		Optional<SocialAccount> linkedAccount =
			socialAccountRepository.findByProviderAndProviderUserId(
				provider,
				providerUserId);

		if (linkedAccount.isPresent()) {
			MemberErrorCode errorCode = linkedAccount.get().getMember().getId().equals(targetMemberId)
				? MemberErrorCode.ACCOUNT_ALREADY_LINKED
				: MemberErrorCode.SOCIAL_ACCOUNT_IN_USE;

			throw new NalssiLogException(errorCode);
		}

		if (socialAccountRepository.findByMemberIdAndProvider(targetMemberId, provider).isPresent()) {
			throw new NalssiLogException(MemberErrorCode.ACCOUNT_ALREADY_LINKED);
		}

		Member member = memberRepository.getMember(targetMemberId);

		try {
			socialAccountRepository.saveAndFlush(
				SocialAccount.link(member, provider, providerUserId, email));
		} catch (DataIntegrityViolationException exception) {
			throw translateSocialLinkConflict(exception);
		}

		return memberRepository.getMemberInfo(targetMemberId);
	}

	/** 최종 로그인 세션 발급에 사용한 소셜 계정의 로그인 시각만 갱신한다. */
	@Transactional
	public void recordLogin(Long memberId, Provider provider) {
		SocialAccount account = socialAccountRepository
			.findByMemberIdAndProvider(memberId, provider)
			.orElseThrow(() -> new NalssiLogException(
				MemberErrorCode.SOCIAL_ACCOUNT_NOT_FOUND));

		account.touchLogin();
	}

	/** 회원 탈퇴. 익명화 + 소셜 삭제 + 제보 익명화용 MemberWithdrawnEvent 발행. 세션·쿠키 정리는 auth. */
	@Transactional
	public void withdraw(Long memberId) {
		Member member = memberRepository.getMember(memberId);

		member.withdraw();
		socialAccountRepository.deleteAllByMemberId(memberId);

		eventPublisher.publishEvent(MemberWithdrawnEvent.of(memberId));
	}

	public MemberInfo getMemberInfo(Long memberId) {
		return memberRepository.getMemberInfo(memberId);
	}

	public Optional<MemberInfo> findMemberInfo(Long memberId) {
		return memberRepository.findMemberInfo(memberId);
	}

	public List<MemberSummary> findMemberSummaries(Collection<Long> memberIds) {
		return memberRepository.findSummariesByIds(memberIds);
	}

	public Optional<MemberInfo> findMemberInfo(
		Provider provider,
		String providerUserId
	) {
		return socialAccountRepository
			.findByProviderAndProviderUserId(provider, providerUserId)
			.map(account -> memberRepository.getMemberInfo(account.getMember().getId()));
	}
}
