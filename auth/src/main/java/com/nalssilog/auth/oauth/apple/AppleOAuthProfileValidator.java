package com.nalssilog.auth.oauth.apple;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("apple")
@RequiredArgsConstructor
public class AppleOAuthProfileValidator {

    private final AppleOAuthProperties properties;

    @PostConstruct
    void validate() {
        properties.requireConfigured();
    }
}
