package com.nalssilog.auth.mobile.oauth;

import com.nalssilog.member.domain.Provider;

public record MobileOAuthTransaction(
        MobileOAuthPurpose purpose,
        Provider provider,
        String redirectUri,
        String codeChallenge,
        String appState,
        String referenceId,
        Long targetMemberId
) {
}
