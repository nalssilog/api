package com.nalssilog.auth.mobile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobileRefreshRequest(
        @NotBlank @Size(max = 500) String refreshToken,
        @Valid @NotNull MobileDeviceRequest device
) {
}
