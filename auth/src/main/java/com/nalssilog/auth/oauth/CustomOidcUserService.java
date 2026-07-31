package com.nalssilog.auth.oauth;

import com.nalssilog.auth.member.MemberClient;
import com.nalssilog.auth.oauth.apple.AppleAuthorizationUserContext;
import com.nalssilog.member.application.dto.SocialLoginResult;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Loads OpenID Connect providers, including Sign in with Apple, through the same member resolution
 * policy used by classic OAuth2 providers.
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final MemberClient memberClient;
    private final AppleAuthorizationUserContext appleAuthorizationUserContext;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
            throws OAuth2AuthenticationException {
        OidcUser oidcUser = Objects.requireNonNull(
                super.loadUser(userRequest),
                "OidcUser must not be null");
        String registrationId =
                userRequest.getClientRegistration().getRegistrationId();
        OAuthUserInfo userInfo = OAuthUserInfo.of(
                registrationId,
                oidcUser.getClaims(),
                appleAuthorizationUserContext.currentSocialName().orElse(null));
        SocialLoginResult result =
                memberClient.resolveSocialLogin(userInfo);

        CustomOAuth2UserService.requireEmailForOnboarding(
                result,
                userInfo.email());

        return new SocialOidcPrincipal(result, userInfo, oidcUser);
    }
}
