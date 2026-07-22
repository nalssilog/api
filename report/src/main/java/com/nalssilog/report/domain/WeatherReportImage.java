package com.nalssilog.report.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 제보 이미지. 업로드는 presigned URL 방식이라 백엔드는 storage_key 만 보관한다(실제 파일은 스토리지).
 */
@Entity
@Table(name = "weather_report_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherReportImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private WeatherReport report;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public static WeatherReportImage create(WeatherReport report, String storageKey, int displayOrder) {
        WeatherReportImage image = new WeatherReportImage();
        image.report = report;
        image.storageKey = storageKey;
        image.displayOrder = displayOrder;

        return image;
    }
}
