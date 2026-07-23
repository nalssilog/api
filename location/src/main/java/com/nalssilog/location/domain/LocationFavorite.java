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
 * 회원의 즐겨찾기 지역. 회원은 다른 모듈이라 ID(Long)로만 참조한다.
 */
@Entity
@Table(name = "location_favorite",
        indexes = @Index(name = "idx_location_favorite_member", columnList = "member_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_location_favorite_member_location",
                columnNames = {"member_id", "location_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocationFavorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    public static LocationFavorite of(Long memberId, Long locationId) {
        LocationFavorite favorite = new LocationFavorite();
        favorite.memberId = memberId;
        favorite.locationId = locationId;

        return favorite;
    }
}
