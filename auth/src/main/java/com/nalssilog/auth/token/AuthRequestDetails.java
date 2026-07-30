package com.nalssilog.auth.token;

import com.nalssilog.member.domain.Provider;

public record AuthRequestDetails(
        Provider provider,
        String sessionId,
        CredentialTransport transport
) {
}
