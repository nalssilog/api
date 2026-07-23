package com.nalssilog.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.nalssilog.member.application.dto.SocialLoginResult;
import com.nalssilog.member.domain.MemberStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class CustomOAuth2UserServiceTest {

    @Test
    void newMemberWithoutEmailIsRejected() {
        SocialLoginResult result = SocialLoginResult.newMember(null);

        OAuth2AuthenticationException exception = catchThrowableOfType(
                OAuth2AuthenticationException.class,
                () -> CustomOAuth2UserService.requireEmailForOnboarding(result, null)
        );

        assertThat(exception.getError().getErrorCode())
                .isEqualTo(CustomOAuth2UserService.EMAIL_REQUIRED_ERROR);
    }

    @Test
    void existingMemberCanLoginWithoutEmailBeingReturnedAgain() {
        assertThatCode(() -> CustomOAuth2UserService.requireEmailForOnboarding(
                SocialLoginResult.existing(1L, MemberStatus.ACTIVE), null))
                .doesNotThrowAnyException();
    }
}
