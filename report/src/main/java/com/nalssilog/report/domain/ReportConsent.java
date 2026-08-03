package com.nalssilog.report.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
import com.nalssilog.member.domain.TermsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비회원 제보 시 동의한 필수 약관의 종류, 문서 버전, 서버 수신 시각을 보관한다.
 */
@Entity
@Table(name = "report_consent",
        indexes = @Index(name = "idx_report_consent_report", columnList = "report_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_report_consent_report_type",
                columnNames = {"report_id", "terms_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportConsent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_report_consent_report"))
    private WeatherReport report;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", nullable = false, length = 20)
    private TermsType termsType;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "agreed_at", nullable = false, updatable = false)
    private Instant agreedAt;

    static ReportConsent agree(
            WeatherReport report,
            TermsType termsType,
            String version,
            Instant agreedAt
    ) {
        ReportConsent consent = new ReportConsent();

        consent.report = report;
        consent.termsType = termsType;
        consent.version = version;
        consent.agreedAt = agreedAt;

        return consent;
    }
}
