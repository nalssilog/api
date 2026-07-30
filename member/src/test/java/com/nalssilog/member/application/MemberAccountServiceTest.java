package com.nalssilog.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.member.application.dto.SocialLoginResult;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import com.nalssilog.member.repository.MemberRepository;
import com.nalssilog.member.repository.SocialAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

@SuppressWarnings("java:S5960")
class MemberAccountServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SocialAccountRepository socialAccountRepository =
            mock(SocialAccountRepository.class);
    private final MemberAccountService service = new MemberAccountService(
            memberRepository,
            socialAccountRepository,
            mock(ApplicationEventPublisher.class));

    @Test
    void resolvingExistingSocialIdentityDoesNotRecordServiceLogin() {
        Member member = mock(Member.class);
        SocialAccount account = mock(SocialAccount.class);

        when(socialAccountRepository.findByProviderAndProviderUserId(
                Provider.NAVER,
                "naver-user")).thenReturn(Optional.of(account));
        when(account.getMember()).thenReturn(member);
        when(member.getId()).thenReturn(7L);
        when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);

        SocialLoginResult result = service.resolveSocialLogin(
                Provider.NAVER,
                "naver-user",
                "user@example.com");

        assertThat(result.outcome()).isEqualTo(SocialLoginResult.Outcome.EXISTING);
        assertThat(result.memberId()).isEqualTo(7L);
        verify(account, never()).touchLogin();
    }

    @Test
    void finalServiceLoginRecordsSelectedSocialProvider() {
        SocialAccount account = mock(SocialAccount.class);

        when(socialAccountRepository.findByMemberIdAndProvider(
                7L,
                Provider.KAKAO)).thenReturn(Optional.of(account));

        service.recordLogin(7L, Provider.KAKAO);

        verify(account).touchLogin();
    }
}
