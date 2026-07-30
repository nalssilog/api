package com.nalssilog.auth.oauth.apple;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AppleAuthorizationUserContext {

    private final ThreadLocal<String> socialName = new ThreadLocal<>();

    public void set(String value) {
        socialName.remove();
        if (value != null && !value.isBlank()) {
            socialName.set(value);
        }
    }

    public Optional<String> currentSocialName() {

        return Optional.ofNullable(socialName.get());
    }

    public void clear() {
        socialName.remove();
    }
}
