package com.nalssilog.auth.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MobileDeviceRequest(
        @NotNull MobilePlatform platform,
        @NotBlank
        @Size(max = 60)
        @Pattern(regexp = "^[^\\p{Cntrl}]+$")
        String deviceName,
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^[0-9A-Za-z][0-9A-Za-z._+() -]*$")
        String appVersion
) {
}
