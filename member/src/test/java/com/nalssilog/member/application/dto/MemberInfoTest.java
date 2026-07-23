package com.nalssilog.member.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class MemberInfoTest {

    @Test
    void unloggedLinkedAccountDoesNotReplaceLastLoginProvider() {
        Member member = Member.register("user@example.com", "이름", "인사하는감자123");
        SocialAccount registered = SocialAccount.register(
                member, Provider.KAKAO, "kakao-id", "user@example.com");
        SocialAccount linked = SocialAccount.link(
                member, Provider.NAVER, "naver-id", "user@example.com");

        MemberInfo info = MemberInfo.of(member, List.of(registered, linked));

        assertThat(info.lastLoginProvider()).isEqualTo(Provider.KAKAO);
        assertThat(info.connectedProviders()).containsExactly(Provider.KAKAO, Provider.NAVER);
    }
}
