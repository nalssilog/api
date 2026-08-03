package com.nalssilog.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.SocialLoginResult;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberErrorCode;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.MemberRole;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import com.nalssilog.member.repository.MemberRepository;
import com.nalssilog.member.repository.MemberRoleChangeJpaRepository;
import com.nalssilog.member.repository.SocialAccountRepository;
import java.sql.SQLException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@SuppressWarnings("java:S5960")
class MemberAccountServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SocialAccountRepository socialAccountRepository =
            mock(SocialAccountRepository.class);
    private final MemberRoleChangeJpaRepository roleChangeRepository =
            mock(MemberRoleChangeJpaRepository.class);
    private final MemberAccountService service = new MemberAccountService(
            memberRepository,
            socialAccountRepository,
            mock(ApplicationEventPublisher.class),
            roleChangeRepository);

    @Test
    void lastAdminCannotBeDemoted() {
        Member admin = Member.register("admin@example.com", "관리자", "관리자01");

        admin.changeRole(MemberRole.ADMIN);
        when(memberRepository.getMember(7L)).thenReturn(admin);
        when(memberRepository.countByRole(MemberRole.ADMIN)).thenReturn(1L);

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.changeRole(7L, MemberRole.MEMBER, 9L));

        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.LAST_ADMIN_REQUIRED);
        assertThat(admin.getRole()).isEqualTo(MemberRole.ADMIN);
        verify(roleChangeRepository, never()).save(any());
    }

    @Test
    void roleChangeIsPersistedAsAuditRecord() {
        Member member = Member.register("user@example.com", "회원", "회원01");

        when(memberRepository.getMember(7L)).thenReturn(member);

        service.changeRole(7L, MemberRole.MODERATOR, 9L);

        assertThat(member.getRole()).isEqualTo(MemberRole.MODERATOR);
        verify(roleChangeRepository).save(any());
    }

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

    @Test
    void rejectsLinkWhenMemberAlreadyHasProvider() {
        when(socialAccountRepository.findByProviderAndProviderUserId(
                Provider.KAKAO,
                "new-kakao-user")).thenReturn(Optional.empty());
        when(socialAccountRepository.findByMemberIdAndProvider(
                7L,
                Provider.KAKAO)).thenReturn(Optional.of(mock(SocialAccount.class)));

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.linkSocial(
                        7L,
                        Provider.KAKAO,
                        "new-kakao-user",
                        "user@example.com"));

        assertThat(exception.getErrorCode())
                .isEqualTo(MemberErrorCode.ACCOUNT_ALREADY_LINKED);
        verify(socialAccountRepository, never()).saveAndFlush(any());
    }

    @Test
    void translatesConcurrentProviderUserCollisionToDomainConflict() {
        Member member = mock(Member.class);

        when(socialAccountRepository.findByProviderAndProviderUserId(
                Provider.KAKAO,
                "kakao-user")).thenReturn(Optional.empty());
        when(socialAccountRepository.findByMemberIdAndProvider(
                7L,
                Provider.KAKAO)).thenReturn(Optional.empty());
        when(memberRepository.getMember(7L)).thenReturn(member);
        when(socialAccountRepository.saveAndFlush(any(SocialAccount.class)))
                .thenThrow(constraintCollision(
                        "uk_social_account_provider_user"));

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.linkSocial(
                        7L,
                        Provider.KAKAO,
                        "kakao-user",
                        "user@example.com"));

        assertThat(exception.getErrorCode())
                .isEqualTo(MemberErrorCode.SOCIAL_ACCOUNT_IN_USE);
    }

    private static DataIntegrityViolationException constraintCollision(
            String constraintName
    ) {
        return new DataIntegrityViolationException(
                "duplicate social account",
                new ConstraintViolationException(
                        "duplicate social account",
                        new SQLException(),
                        constraintName));
    }
}
