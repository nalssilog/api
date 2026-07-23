package com.nalssilog.auth.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalssilog.member.domain.Provider;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class OAuthUserInfoTest {

    private static final String EMAIL_KEY = "email";
    private static final String NICKNAME_KEY = "nickname";
    private static final String USER_EMAIL = "user@example.com";
    private static final String NAVER_NICKNAME = "네이버 별명";

    @Test
    void googleProfileNameBecomesSocialName() {
        OAuthUserInfo userInfo = OAuthUserInfo.of("google", Map.of(
                "sub", "google-id",
                EMAIL_KEY, USER_EMAIL,
                "name", "구글 이름"
        ));

        assertThat(userInfo).isEqualTo(new OAuthUserInfo(
                Provider.GOOGLE, "google-id", USER_EMAIL, "구글 이름"));
    }

    @Test
    void kakaoProfileNicknameBecomesSocialName() {
        OAuthUserInfo userInfo = OAuthUserInfo.of("kakao", Map.of(
                "id", 1234L,
                "kakao_account", Map.of(
                        EMAIL_KEY, USER_EMAIL,
                        "profile", Map.of(NICKNAME_KEY, "카카오 이름")
                )
        ));

        assertThat(userInfo).isEqualTo(new OAuthUserInfo(
                Provider.KAKAO, "1234", USER_EMAIL, "카카오 이름"));
    }

    @Test
    void naverUsesNameBeforeNickname() {
        OAuthUserInfo userInfo = OAuthUserInfo.of("naver", Map.of(
                "response", Map.of(
                        "id", "naver-id",
                        EMAIL_KEY, USER_EMAIL,
                        "name", "네이버 이름",
                        NICKNAME_KEY, NAVER_NICKNAME
                )
        ));

        assertThat(userInfo.socialName()).isEqualTo("네이버 이름");
    }

    @Test
    void naverFallsBackToNicknameWhenNameIsBlank() {
        OAuthUserInfo userInfo = OAuthUserInfo.of("naver", Map.of(
                "response", Map.of(
                        "id", "naver-id",
                        "name", " ",
                        NICKNAME_KEY, NAVER_NICKNAME
                )
        ));

        assertThat(userInfo.socialName()).isEqualTo(NAVER_NICKNAME);
    }
}
