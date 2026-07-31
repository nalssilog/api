package com.nalssilog.auth.mobile.guest;

import com.nalssilog.auth.config.AuthProperties;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobileGuestCredentialCleanupService {

    private final MobileGuestCredentialRepository repository;
    private final AuthProperties properties;

    @Scheduled(
            fixedDelayString = "${nalssilog.auth.guest.cleanup-interval:6h}",
            initialDelayString = "${nalssilog.auth.guest.cleanup-initial-delay:1m}")
    @Transactional
    public void cleanupExpiredCredentials() {
        cleanupExpiredCredentialsAt(Instant.now());
    }

    int cleanupExpiredCredentialsAt(Instant now) {
        Instant cutoff = now.minus(properties.guest().expiredRetention());
        int deleted = repository.deleteByExpiresAtLessThanEqual(cutoff);

        if (deleted > 0) {
            log.info("auth.guest.expired_credentials_deleted count={}", deleted);
        }

        return deleted;
    }
}
