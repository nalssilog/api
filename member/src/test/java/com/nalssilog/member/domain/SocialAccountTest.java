package com.nalssilog.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class SocialAccountTest {

    private static final String EMAIL = "user@example.com";

    @Test
    void registrationRecordsLoginButAdditionalLinkDoesNot() {
        Member member = Member.register(EMAIL, "이름", "인사하는감자123");

        SocialAccount registered = SocialAccount.register(member, Provider.KAKAO, "kakao-id", EMAIL);
        SocialAccount linked = SocialAccount.link(member, Provider.NAVER, "naver-id", EMAIL);

        assertThat(registered.getLastLoginAt()).isNotNull();
        assertThat(linked.getLastLoginAt()).isNull();
    }

    @Test
    void linkedAccountGetsLoginTimeOnlyAfterActualLogin() {
        Member member = Member.register(EMAIL, "이름", "인사하는감자123");
        SocialAccount linked = SocialAccount.link(member, Provider.NAVER, "naver-id", EMAIL);

        linked.touchLogin();

        assertThat(linked.getLastLoginAt()).isNotNull();
    }
}
