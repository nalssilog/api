package com.nalssilog.auth.mobile;

import com.nalssilog.auth.device.DeviceInfo;
import com.nalssilog.auth.device.DeviceInfoResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MobileDeviceInfoResolver {

    private static final int MAX_NAME_LENGTH = 60;

    private final DeviceInfoResolver deviceInfoResolver;

    public DeviceInfo resolve(
            MobileDeviceRequest device,
            HttpServletRequest request
    ) {
        String label = device.platform().name()
                + " · " + device.deviceName().strip()
                + " · " + device.appVersion().strip();

        if (label.length() > MAX_NAME_LENGTH) {
            label = label.substring(0, MAX_NAME_LENGTH);
        }

        return new DeviceInfo(label, deviceInfoResolver.resolveIp(request));
    }
}
