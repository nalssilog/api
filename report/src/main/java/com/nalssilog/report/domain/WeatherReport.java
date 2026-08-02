package com.nalssilog.report.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
import com.nalssilog.member.domain.TermsType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 동네 체감 날씨 제보. 다른 모듈 엔티티(회원·지역)는 ID(Long)로만 참조한다.
 * 작성자는 회원(authorMemberId) 또는 익명(authorAnonymousKey) 둘 중 하나.
 */
@Entity
@Table(name = "weather_report",
        indexes = @Index(name = "idx_weather_report_location_created", columnList = "location_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, length = 20)
    private ActorType authorType;

    @Column(name = "author_member_id", nullable = true)
    private Long authorMemberId;

    @Column(name = "author_anonymous_key", nullable = true, length = 36)
    private String authorAnonymousKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "temperature", nullable = false, length = 20)
    private Temperature temperature;

    @Enumerated(EnumType.STRING)
    @Column(name = "precipitation", nullable = false, length = 20)
    private Precipitation precipitation;

    @Enumerated(EnumType.STRING)
    @Column(name = "sunlight", nullable = false, length = 20)
    private Sunlight sunlight;

    @Column(name = "comment", nullable = true, length = 200)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private ModerationStatus moderationStatus;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeatherReportImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportConsent> consents = new ArrayList<>();

    public static WeatherReport ofMember(Long locationId, Long memberId, Temperature temperature,
                                         Precipitation precipitation, Sunlight sunlight, String comment) {
        WeatherReport report = base(locationId, temperature, precipitation, sunlight, comment);

        report.authorType = ActorType.MEMBER;
        report.authorMemberId = memberId;

        return report;
    }

    public static WeatherReport ofAnonymous(Long locationId, String anonymousKey, Temperature temperature,
                                            Precipitation precipitation, Sunlight sunlight, String comment) {
        WeatherReport report = base(locationId, temperature, precipitation, sunlight, comment);

        report.authorType = ActorType.ANONYMOUS;
        report.authorAnonymousKey = anonymousKey;

        return report;
    }

    public void addImages(List<String> storageKeys) {
        for (int order = 0; order < storageKeys.size(); order++) {
            images.add(WeatherReportImage.create(this, storageKeys.get(order), order));
        }
    }

    public void addConsent(TermsType termsType, String version, Instant agreedAt) {
        if (authorType != ActorType.ANONYMOUS) {
            throw new IllegalStateException("Only anonymous reports can record report consent.");
        }

        boolean alreadyRecorded = consents.stream()
                .anyMatch(consent -> consent.getTermsType() == termsType);

        if (alreadyRecorded) {
            throw new IllegalArgumentException("A report consent type can only be recorded once.");
        }

        consents.add(ReportConsent.agree(this, termsType, version, agreedAt));
    }

    private static WeatherReport base(Long locationId, Temperature temperature, Precipitation precipitation,
                                      Sunlight sunlight, String comment) {
        WeatherReport report = new WeatherReport();

        report.locationId = locationId;
        report.temperature = temperature;
        report.precipitation = precipitation;
        report.sunlight = sunlight;
        report.comment = comment;
        report.moderationStatus = ModerationStatus.VISIBLE;

        return report;
    }

    public void hide() {
        this.moderationStatus = ModerationStatus.HIDDEN;
    }

    public void restore() {
        this.moderationStatus = ModerationStatus.VISIBLE;
    }

    public void removeByModerator() {
        this.moderationStatus = ModerationStatus.REMOVED;
    }
}
