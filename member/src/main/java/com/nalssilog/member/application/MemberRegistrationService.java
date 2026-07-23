package com.nalssilog.member.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberErrorCode;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import com.nalssilog.member.domain.event.MemberRegisteredEvent;
import com.nalssilog.member.repository.MemberRepository;
import com.nalssilog.member.repository.SocialAccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 신규 회원 가입. 닉네임 유니크 충돌이 발생하면 전체 가입 트랜잭션을 롤백하고 새 닉네임으로 재시도한다.
 */
@Service
@RequiredArgsConstructor
public class MemberRegistrationService {

    private static final int MAX_NICKNAME_ATTEMPTS = 50;
    private static final String NICKNAME_CONSTRAINT = "uk_member_nickname";

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final ConsentService consentService;
    private final ApplicationEventPublisher eventPublisher;
    private final NicknameGenerator nicknameGenerator;
    private final PlatformTransactionManager transactionManager;

    public MemberInfo registerMember(Provider provider, String providerUserId, String email,
                                     String socialName, List<TermsAgreement> agreedTerms) {
        String name = normalizeSocialName(socialName);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        for (int attempt = 0; attempt < MAX_NICKNAME_ATTEMPTS; attempt++) {
            String nickname = nicknameGenerator.generate();

            try {
                return transaction.execute(status ->
                        registerInTransaction(provider, providerUserId, email, name, nickname, agreedTerms));
            } catch (DataIntegrityViolationException e) {
                if (!isNicknameCollision(e)) {
                    throw e;
                }
            }
        }

        throw new NalssiLogException(MemberErrorCode.NICKNAME_GENERATION_FAILED);
    }

    private MemberInfo registerInTransaction(Provider provider, String providerUserId, String email,
                                             String name, String nickname, List<TermsAgreement> agreedTerms) {
        Member member = memberRepository.saveAndFlush(Member.register(email, name, nickname));
        socialAccountRepository.save(SocialAccount.link(member, provider, providerUserId, email));
        consentService.recordOnboardingConsents(member.getId(), agreedTerms);

        eventPublisher.publishEvent(MemberRegisteredEvent.of(member.getId(), provider));

        return memberRepository.getMemberInfo(member.getId());
    }

    private static String normalizeSocialName(String socialName) {
        if (socialName == null || socialName.isBlank()) {
            return "";
        }

        String stripped = socialName.strip();
        int codePointCount = stripped.codePointCount(0, stripped.length());

        if (codePointCount <= Member.NAME_MAX_LENGTH) {
            return stripped;
        }

        return stripped.substring(0, stripped.offsetByCodePoints(0, Member.NAME_MAX_LENGTH));
    }

    private static boolean isNicknameCollision(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();

                return constraintName != null && NICKNAME_CONSTRAINT.equalsIgnoreCase(constraintName);
            }
        }

        return false;
    }
}
