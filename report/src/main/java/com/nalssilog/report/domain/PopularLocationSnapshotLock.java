package com.nalssilog.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 여러 애플리케이션 인스턴스의 인기 지역 스냅샷 갱신을 직렬화하는 DB 잠금 행.
 */
@Entity
@Table(name = "popular_location_snapshot_lock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularLocationSnapshotLock {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;
}
