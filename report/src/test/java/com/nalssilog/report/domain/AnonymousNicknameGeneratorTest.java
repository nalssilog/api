package com.nalssilog.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class AnonymousNicknameGeneratorTest {

    @Test
    void generatesStableTenDigitAnonymousNickname() {
        String first = AnonymousNicknameGenerator.generate("anonymous-id");
        String second = AnonymousNicknameGenerator.generate("anonymous-id");

        assertThat(first)
                .isEqualTo(second)
                .matches("익명#[0-9]{10}");
    }

    @Test
    void differentAnonymousIdentitiesGetDifferentNicknames() {
        assertThat(AnonymousNicknameGenerator.generate("anonymous-id-1"))
                .isNotEqualTo(AnonymousNicknameGenerator.generate("anonymous-id-2"));
    }
}
