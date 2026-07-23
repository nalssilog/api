package com.nalssilog.member.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class MemberMeResponseTest {

    @Test
    void currentProviderComesFromCurrentAuthenticationNotGlobalLastLogin() {
        MemberInfo member = new MemberInfo(
                1L,
                "인사하는감자123",
                "이름",
                "user@example.com",
                AvatarType.PRESET,
                "avatar-01",
                MemberStatus.ACTIVE,
                Provider.NAVER,
                List.of(Provider.KAKAO, Provider.NAVER)
        );

        MemberMeResponse response = MemberMeResponse.from(member, Provider.KAKAO);

        assertThat(response.currentProvider()).isEqualTo(Provider.KAKAO);
    }
}
