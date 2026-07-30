package com.nalssilog.auth.mobile.guest;

import com.nalssilog.auth.device.DeviceInfoResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/guests")
@RequiredArgsConstructor
public class MobileGuestController {

    private final MobileGuestCredentialService credentialService;
    private final DeviceInfoResolver deviceInfoResolver;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuestCredentialResponse issue(HttpServletRequest request) {

        return GuestCredentialResponse.from(
                credentialService.issue(deviceInfoResolver.resolveIp(request)));
    }
}
