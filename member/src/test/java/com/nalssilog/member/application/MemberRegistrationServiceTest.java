package com.nalssilog.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.repository.MemberRepository;
import com.nalssilog.member.repository.SocialAccountRepository;
import java.sql.SQLException;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class MemberRegistrationServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
    private final ConsentService consentService = mock(ConsentService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final NicknameGenerator nicknameGenerator = mock(NicknameGenerator.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

    private MemberRegistrationService service;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        service = new MemberRegistrationService(
                memberRepository,
                socialAccountRepository,
                consentService,
                eventPublisher,
                nicknameGenerator,
                transactionManager
        );
    }

    @Test
    void retriesWholeRegistrationWithNewNicknameOnUniqueConstraintCollision() {
        MemberInfo expected = new MemberInfo(
                1L, "인사하는감자124", "소셜 이름", "user@example.com",
                AvatarType.PRESET, "avatar-01", MemberStatus.ACTIVE, Provider.KAKAO, List.of(Provider.KAKAO));
        DataIntegrityViolationException collision = new DataIntegrityViolationException(
                "duplicate nickname",
                new ConstraintViolationException(
                        "duplicate nickname", new SQLException(), "uk_member_nickname")
        );

        when(nicknameGenerator.generate()).thenReturn("인사하는감자123", "인사하는감자124");
        when(memberRepository.saveAndFlush(any(Member.class)))
                .thenThrow(collision)
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.getMemberInfo(isNull())).thenReturn(expected);

        MemberInfo actual = service.registerMember(
                Provider.KAKAO, "provider-id", "user@example.com", "  소셜 이름  ", List.<TermsAgreement>of());

        assertThat(actual).isSameAs(expected);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository, times(2)).saveAndFlush(memberCaptor.capture());
        assertThat(memberCaptor.getAllValues())
                .extracting(Member::getNickname)
                .containsExactly("인사하는감자123", "인사하는감자124");
        assertThat(memberCaptor.getAllValues().get(1).getName()).isEqualTo("소셜 이름");
        verify(transactionManager).rollback(any());
        verify(transactionManager).commit(any());
    }

    @Test
    void storesEmptyNameWhenSocialProviderDoesNotReturnOne() {
        MemberInfo expected = new MemberInfo(
                1L, "인사하는감자123", "", null,
                AvatarType.PRESET, "avatar-01", MemberStatus.ACTIVE, Provider.NAVER, List.of(Provider.NAVER));
        when(nicknameGenerator.generate()).thenReturn("인사하는감자123");
        when(memberRepository.saveAndFlush(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.getMemberInfo(isNull())).thenReturn(expected);

        service.registerMember(Provider.NAVER, "provider-id", null, null, List.of());

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getName()).isEmpty();
    }

    @Test
    void stripsAndLimitsSocialNameToThirtyCharacters() {
        String longName = "  " + "가".repeat(31) + "  ";
        MemberInfo expected = new MemberInfo(
                1L, "인사하는감자123", "가".repeat(30), null,
                AvatarType.PRESET, "avatar-01", MemberStatus.ACTIVE, Provider.GOOGLE, List.of(Provider.GOOGLE));
        when(nicknameGenerator.generate()).thenReturn("인사하는감자123");
        when(memberRepository.saveAndFlush(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.getMemberInfo(isNull())).thenReturn(expected);

        service.registerMember(Provider.GOOGLE, "provider-id", null, longName, List.of());

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getName()).isEqualTo("가".repeat(30));
    }
}
