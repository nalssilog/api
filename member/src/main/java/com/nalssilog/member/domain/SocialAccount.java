package com.nalssilog.member.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "social_account", uniqueConstraints = {
        @UniqueConstraint(name = "uk_social_account_provider_user", columnNames = {"provider", "provider_user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "provider_email", nullable = true, length = 255)
    private String providerEmail;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** 최초 가입은 방금 소셜 인증을 완료한 상태이므로 로그인 시각을 기록한다. */
    public static SocialAccount register(Member member, Provider provider, String providerUserId,
                                         String providerEmail) {
        SocialAccount account = create(member, provider, providerUserId, providerEmail);
        account.lastLoginAt = Instant.now();

        return account;
    }

    /** 추가 연동은 로그인이 아니므로 실제로 이 제공자로 로그인하기 전까지 로그인 시각을 비워 둔다. */
    public static SocialAccount link(Member member, Provider provider, String providerUserId, String providerEmail) {
        return create(member, provider, providerUserId, providerEmail);
    }

    private static SocialAccount create(Member member, Provider provider, String providerUserId,
                                        String providerEmail) {
        SocialAccount account = new SocialAccount();
        account.member = member;
        account.provider = provider;
        account.providerUserId = providerUserId;
        account.providerEmail = providerEmail;

        return account;
    }

    public void touchLogin() {
        this.lastLoginAt = Instant.now();
    }
}
