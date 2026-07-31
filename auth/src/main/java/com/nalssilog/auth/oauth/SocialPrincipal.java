package com.nalssilog.auth.oauth;

import com.nalssilog.member.application.dto.SocialLoginResult;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Provider-neutral principal passed from OAuth2/OIDC user loading to the completion service.
 */
public interface SocialPrincipal extends OAuth2User {

    SocialLoginResult result();

    OAuthUserInfo userInfo();
}
