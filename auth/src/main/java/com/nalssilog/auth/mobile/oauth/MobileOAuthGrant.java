package com.nalssilog.auth.mobile.oauth;

import com.nalssilog.member.domain.Provider;

public record MobileOAuthGrant(
        MobileAuthResult result,
        Provider provider,
        Long memberId,
        String ticketId,
        String errorCode,
        boolean issueTokens
) {

    public static MobileOAuthGrant success(Long memberId, Provider provider) {

        return new MobileOAuthGrant(
                MobileAuthResult.SUCCESS, provider, memberId, null, null, true);
    }

    public static MobileOAuthGrant signupRequired(Provider provider, String ticketId) {

        return new MobileOAuthGrant(
                MobileAuthResult.SIGNUP_REQUIRED, provider, null, ticketId, null, false);
    }

    public static MobileOAuthGrant linkRequired(Provider provider, String ticketId) {

        return new MobileOAuthGrant(
                MobileAuthResult.LINK_REQUIRED, provider, null, ticketId, null, false);
    }

    public static MobileOAuthGrant linkSuccess(
            Long memberId,
            Provider provider,
            boolean issueTokens
    ) {

        return new MobileOAuthGrant(
                MobileAuthResult.LINK_SUCCESS, provider, memberId, null, null, issueTokens);
    }

    public static MobileOAuthGrant failed(Provider provider, String errorCode) {

        return new MobileOAuthGrant(
                MobileAuthResult.FAILED, provider, null, null, errorCode, false);
    }
}
