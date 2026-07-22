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
 * 행정동 단위 기준 지역. 위·경도는 저장하되 프론트 응답에는 노출하지 않는다(id + label 만).
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

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    public static Location of(String adminCode, String sido, String sigungu, String dong,
                              double latitude, double longitude) {
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
