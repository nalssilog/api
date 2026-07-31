package com.nalssilog.auth.mobile.guest;

import com.nalssilog.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mobile_guest_credential", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mobile_guest_token_hash", columnNames = "token_hash"),
        @UniqueConstraint(name = "uk_mobile_guest_anonymous_key", columnNames = "anonymous_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileGuestCredential extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "anonymous_key", nullable = false, length = 36, updatable = false)
    private String anonymousKey;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public static MobileGuestCredential issue(
            String tokenHash,
            String anonymousKey,
            Instant expiresAt
    ) {
        MobileGuestCredential credential = new MobileGuestCredential();

        credential.tokenHash = tokenHash;
        credential.anonymousKey = anonymousKey;
        credential.expiresAt = expiresAt;

        return credential;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke() {
        if (revokedAt == null) {
            revokedAt = Instant.now();
        }
    }
}
