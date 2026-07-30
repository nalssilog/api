package com.nalssilog.auth.mobile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobileTokenRequest(
        @NotBlank @Size(max = 200) String code,
        @NotBlank @Size(min = 43, max = 128) String codeVerifier,
        @NotBlank @Size(max = 500) String redirectUri,
        @Valid @NotNull MobileDeviceRequest device
) {
}
