package com.nalssilog.member.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 특정 약관 종류의 특정 버전에 동의한 기록. 약관 문구는 저장하지 않는다(프론트 관리).
 * version 은 프론트가 알려주는 값이며, 어느 버전에 동의했는지 법적 근거로 남긴다.
 */
@Entity
@Table(name = "member_consent",
        indexes = @Index(name = "idx_member_consent_member", columnList = "member_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_member_consent_member_type", columnNames = {"member_id", "terms_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberConsent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", nullable = false, length = 20)
    private TermsType termsType;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "agreed", nullable = false)
    private boolean agreed;

    @Column(name = "agreed_at", nullable = false)
    private Instant agreedAt;

    public static MemberConsent agree(Long memberId, TermsType termsType, String version) {
        MemberConsent consent = new MemberConsent();
        consent.memberId = memberId;
        consent.termsType = termsType;
        consent.version = version;
        consent.agreed = true;
        consent.agreedAt = Instant.now();

        return consent;
    }
}
