package com.nalssilog.auth.oauth;

import com.nalssilog.auth.member.MemberClient;
import com.nalssilog.member.application.dto.SocialLoginResult;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    public static final String EMAIL_REQUIRED_ERROR = "email_required";

    private final MemberClient memberClient;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = Objects.requireNonNull(super.loadUser(userRequest), "OAuth2User must not be null");
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthUserInfo userInfo = OAuthUserInfo.of(registrationId, oAuth2User.getAttributes());
        SocialLoginResult result = memberClient.resolveSocialLogin(userInfo);

        requireEmailForOnboarding(result, userInfo.email());

        return new SocialAuthPrincipal(result, userInfo, oAuth2User.getAttributes());
    }

    static void requireEmailForOnboarding(SocialLoginResult result, String email) {
        if (result.outcome() != SocialLoginResult.Outcome.EXISTING && !StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(EMAIL_REQUIRED_ERROR),
                    "Social account email is required for signup or account linking"
            );
        }
    }
}
