package com.nalssilog.auth.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MobileLogoutRequest(
        @NotBlank @Size(max = 500) String refreshToken
) {
}
