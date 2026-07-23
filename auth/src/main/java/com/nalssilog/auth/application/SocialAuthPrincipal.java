package com.nalssilog.auth.application;

import com.nalssilog.auth.client.OAuthUserInfo;
import com.nalssilog.member.application.dto.SocialLoginResult;
import com.nalssilog.member.domain.MemberStatus;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * OAuth2 로그인 직후 SuccessHandler 로 전달되는 principal.
 * 소셜 인증 결과(기존/신규/연동필요)와 정규화된 소셜 정보를 함께 실어 보낸다.
 * (일반 API 요청은 JwtAuthenticationFilter 가 memberId 를 principal 로 세팅한다)
 */
public record SocialAuthPrincipal(
        SocialLoginResult result,
        OAuthUserInfo userInfo,
        Map<String, Object> attributes
) implements OAuth2User {

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(roleOf(result.status())));
    }

    @Override
    public String getName() {
        return result.memberId() == null ? "anonymous" : String.valueOf(result.memberId());
    }

    public static String roleOf(MemberStatus status) {
        return status == MemberStatus.ACTIVE ? "ROLE_MEMBER" : "ROLE_PENDING";
    }
}
