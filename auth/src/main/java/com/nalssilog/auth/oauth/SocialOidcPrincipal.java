package com.nalssilog.auth.oauth;

import com.nalssilog.member.application.dto.SocialLoginResult;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * OIDC counterpart of {@link SocialAuthPrincipal}. The signed ID token is retained so Spring's
 * OIDC authentication contract remains intact while the application receives normalized social
 * identity data.
 */
public record SocialOidcPrincipal(
        SocialLoginResult result,
        OAuthUserInfo userInfo,
        OidcUser delegate
) implements SocialPrincipal, OidcUser {

    @Override
    public Map<String, Object> getClaims() {

        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {

        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {

        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {

        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(new SimpleGrantedAuthority(
                SocialAuthPrincipal.roleOf(result.status())));
    }

    @Override
    public String getName() {

        return result.memberId() == null
                ? delegate.getName()
                : String.valueOf(result.memberId());
    }
}
