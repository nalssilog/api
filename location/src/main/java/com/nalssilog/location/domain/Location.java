package com.nalssilog.location.domain;

import com.nalssilog.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 법정동 단위 기준 지역. 전국 사전 데이터에는 좌표가 없고, 카카오 역지오코딩으로 생성된 행에만 존재한다.
 */
@Entity
@Table(name = "location",
        uniqueConstraints = @UniqueConstraint(name = "uk_location_admin_code", columnNames = "admin_code"),
        indexes = @Index(name = "idx_location_dong", columnList = "dong"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_code", nullable = false, length = 20)
    private String adminCode;

    @Column(name = "sido", nullable = false, length = 20)
    private String sido;

    @Column(name = "sigungu", nullable = false, length = 30)
    private String sigungu;

    @Column(name = "dong", nullable = false, length = 30)
    private String dong;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    public static Location of(String adminCode, String sido, String sigungu, String dong,
                              Double latitude, Double longitude) {
        Location location = new Location();

        location.adminCode = adminCode;
        location.sido = sido;
        location.sigungu = sigungu;
        location.dong = dong;
        location.latitude = latitude;
        location.longitude = longitude;

        return location;
    }

    public String label() {
        return sido + " " + sigungu + " " + dong;
    }
}
