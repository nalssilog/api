package com.nalssilog.auth.application;

import com.nalssilog.auth.client.MemberClient;
import com.nalssilog.auth.client.OAuthUserInfo;
import com.nalssilog.member.application.dto.SocialLoginResult;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberClient memberClient;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = Objects.requireNonNull(super.loadUser(userRequest), "OAuth2User must not be null");
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthUserInfo userInfo = OAuthUserInfo.of(registrationId, oAuth2User.getAttributes());
        SocialLoginResult result = memberClient.resolveSocialLogin(userInfo);

        return new SocialAuthPrincipal(result, userInfo, oAuth2User.getAttributes());
    }
}
