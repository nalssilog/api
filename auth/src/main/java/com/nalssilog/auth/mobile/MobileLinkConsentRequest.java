package com.nalssilog.auth.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MobileLinkConsentRequest(
        @NotBlank @Size(max = 200) String linkTicket,
        @NotBlank @Size(max = 500) String redirectUri,
        @NotBlank @Size(min = 43, max = 43) String codeChallenge,
        @NotBlank String codeChallengeMethod,
        @NotBlank @Size(min = 16, max = 256) String state
) {
}
