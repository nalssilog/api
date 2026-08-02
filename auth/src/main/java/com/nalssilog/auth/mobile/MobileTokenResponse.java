package com.nalssilog.auth.mobile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.core.AuthService.SignupResult;
import com.nalssilog.auth.mobile.oauth.MobileAuthResult;
import com.nalssilog.auth.mobile.oauth.MobileOAuthService.ExchangeResult;
import com.nalssilog.auth.token.TokenPair;
import com.nalssilog.auth.web.MeResponse.Avatar;
import com.nalssilog.auth.web.MeResponse.PendingAuth;
import com.nalssilog.auth.web.MeResponse.User;
import com.nalssilog.member.application.dto.MemberInfo;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MobileTokenResponse(
        MobileAuthResult result,
        String tokenType,
        String accessToken,
        Long accessTokenExpiresIn,
        String refreshToken,
        Long refreshTokenExpiresIn,
        User user,
        String signupTicket,
        String linkTicket,
        PendingAuth pendingAuth
) {

    public static MobileTokenResponse from(
            ExchangeResult result,
            AuthProperties properties
    ) {
        TokenPair tokens = result.tokens();

        return new MobileTokenResponse(
                result.result(),
                tokens == null ? null : "Bearer",
                tokens == null ? null : tokens.accessToken(),
                tokens == null ? null : properties.jwt().accessTokenTtl().toSeconds(),
                tokens == null ? null : tokens.refreshToken(),
                tokens == null ? null : tokens.refreshTokenMaxAge().toSeconds(),
                user(result.member()),
                result.signupTicket(),
                result.linkTicket(),
                result.pendingProvider() == null
                        ? null
                        : new PendingAuth(
                                result.pendingProvider(),
                                result.pendingEmail(),
                                result.existingProviders()));
    }

    public static MobileTokenResponse signup(
            SignupResult result,
            AuthProperties properties
    ) {
        TokenPair tokens = result.tokens();

        return new MobileTokenResponse(
                MobileAuthResult.SUCCESS,
                "Bearer",
                tokens.accessToken(),
                properties.jwt().accessTokenTtl().toSeconds(),
                tokens.refreshToken(),
                tokens.refreshTokenMaxAge().toSeconds(),
                user(result.member()),
                null,
                null,
                null);
    }

    private static User user(MemberInfo member) {
        if (member == null) {
            return null;
        }

        return new User(
                String.valueOf(member.id()),
                member.nickname(),
                new Avatar(member.avatarType(), member.avatarValue()),
                member.role());
    }
}
