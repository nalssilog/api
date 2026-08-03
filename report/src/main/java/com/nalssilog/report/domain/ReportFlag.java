package com.nalssilog.report.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.application.dto.ReportActor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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

@Entity
@Table(name = "report_flag",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_flag_report_reporter",
                columnNames = {"report_id", "reporter_type", "reporter_key"}),
        indexes = {
                @Index(name = "idx_report_flag_status_created", columnList = "status, created_at"),
                @Index(name = "idx_report_flag_report", columnList = "report_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportFlag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private WeatherReport report;

    @Enumerated(EnumType.STRING)
    @Column(name = "reporter_type", nullable = false, length = 20)
    private ActorType reporterType;

    @Column(name = "reporter_key", nullable = false, length = 64)
    private String reporterKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReportFlagReason reason;

    @Column(name = "detail", length = 500)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportFlagStatus status;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processed_by_member_id")
    private Long processedByMemberId;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    public static ReportFlag create(
            WeatherReport report,
            ReportActor reporter,
            ReportFlagReason reason,
            String detail
    ) {
        ReportFlag flag = new ReportFlag();

        flag.report = report;
        flag.reporterType = reporter.type();
        flag.reporterKey = reporter.actorKey();
        flag.reason = reason;
        flag.detail = detail;
        flag.status = ReportFlagStatus.PENDING;

        return flag;
    }

    public void process(ReportFlagStatus result, Long adminMemberId, String note) {
        if (status != ReportFlagStatus.PENDING) {
            throw new NalssiLogException(ReportErrorCode.REPORT_FLAG_ALREADY_PROCESSED);
        }

        if (result != ReportFlagStatus.RESOLVED && result != ReportFlagStatus.REJECTED) {
            throw new NalssiLogException(ReportErrorCode.INVALID_REPORT_FLAG_STATUS);
        }

        status = result;
        processedAt = Instant.now();
        processedByMemberId = adminMemberId;
        resolutionNote = note;
    }
}
