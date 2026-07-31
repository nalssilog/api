package com.nalssilog.auth.mobile.guest;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileGuestCredentialRepository extends JpaRepository<MobileGuestCredential, Long> {

    Optional<MobileGuestCredential> findByTokenHash(String tokenHash);

    int deleteByExpiresAtLessThanEqual(Instant cutoff);
}
