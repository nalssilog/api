package com.nalssilog.member.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NicknameGeneratorTest {

    @Test
    void combinesTwoWordsAndThreeDigitNumberWithoutSpaces() {
        NicknameGenerator generator = new NicknameGenerator();

        String nickname = generator.generate();

        assertThat(nickname)
                .matches("\\S+\\d{3}")
                .doesNotContainAnyWhitespaces();
    }
}
