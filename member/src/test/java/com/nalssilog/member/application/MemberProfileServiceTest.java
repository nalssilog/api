package com.nalssilog.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.client.AvatarStorageClient;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.repository.MemberRepository;
import com.nalssilog.member.repository.SocialAccountRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemberProfileServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
    private final AvatarStorageClient avatarStorageClient = mock(AvatarStorageClient.class);
    private final MemberProfileService service = new MemberProfileService(
            memberRepository,
            socialAccountRepository,
            avatarStorageClient
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
}
