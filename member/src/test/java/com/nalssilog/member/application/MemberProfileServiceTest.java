package com.nalssilog.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.client.AvatarStorageClient;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberErrorCode;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import com.nalssilog.member.repository.MemberRepository;
import com.nalssilog.member.repository.SocialAccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class MemberProfileServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
    private final AvatarStorageClient avatarStorageClient = mock(AvatarStorageClient.class);
    private final MemberVisibilityPolicy visibilityPolicy = mock(MemberVisibilityPolicy.class);
    private final MemberProfileService service = new MemberProfileService(
            memberRepository,
            socialAccountRepository,
            avatarStorageClient,
            visibilityPolicy
    );

    @Test
    void changesMemberNameAndReturnsUpdatedProfile() {
        Member member = Member.register("user@example.com", "기존이름", "인사하는감자123");
        MemberInfo expected = new MemberInfo(
                null,
                "인사하는감자123",
                "홍길동",
                "user@example.com",
                AvatarType.PRESET,
                member.getAvatarValue(),
                MemberStatus.ACTIVE,
                null,
                List.of()
        );

        when(memberRepository.getMember(1L)).thenReturn(member);
        when(memberRepository.getMemberInfo(1L)).thenReturn(expected);

        MemberInfo actual = service.changeName(1L, "  홍길동  ");

        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(actual).isSameAs(expected);
    }

    @Test
    void rejectsUnlinkingCurrentLoginProvider() {
        Member member = Member.register("user@example.com", "이름", "인사하는감자123");
        SocialAccount current = SocialAccount.register(
                member, Provider.KAKAO, "kakao-id", "user@example.com");

        when(socialAccountRepository.findByMemberIdAndProvider(1L, Provider.KAKAO))
                .thenReturn(Optional.of(current));

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.unlinkSocial(1L, Provider.KAKAO, Provider.KAKAO)
        );

        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.CURRENT_LOGIN_PROVIDER);
        verify(socialAccountRepository, never()).delete(current);
    }

    @Test
    void unlinksProviderThatIsNotUsedByCurrentLogin() {
        Member member = Member.register("user@example.com", "이름", "인사하는감자123");
        SocialAccount linked = SocialAccount.link(
                member, Provider.NAVER, "naver-id", "user@example.com");

        when(socialAccountRepository.findByMemberIdAndProvider(1L, Provider.NAVER))
                .thenReturn(Optional.of(linked));
        when(socialAccountRepository.countByMemberId(1L)).thenReturn(2L);

        service.unlinkSocial(1L, Provider.NAVER, Provider.KAKAO);

        verify(socialAccountRepository).delete(linked);
    }

    @Test
    void hidesActivePublicProfileWhenBlockRelationExistsInEitherDirection() {
        MemberInfo profile = activeProfile(7L);

        when(memberRepository.findMemberInfo(7L)).thenReturn(Optional.of(profile));
        when(visibilityPolicy.canView(8L, 7L)).thenReturn(false);

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.getPublicProfile(8L, 7L));

        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void returnsActivePublicProfileWhenNoBlockRelationExists() {
        MemberInfo profile = activeProfile(7L);

        when(memberRepository.findMemberInfo(7L)).thenReturn(Optional.of(profile));
        when(visibilityPolicy.canView(8L, 7L)).thenReturn(true);

        assertThat(service.getPublicProfile(8L, 7L)).isSameAs(profile);
    }

    private MemberInfo activeProfile(Long memberId) {
        return new MemberInfo(
                memberId,
                "이웃",
                "이름",
                "user@example.com",
                AvatarType.PRESET,
                "avatar-01",
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                List.of(Provider.KAKAO));
    }
}
