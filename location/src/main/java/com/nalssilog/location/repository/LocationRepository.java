package com.nalssilog.location.repository;

import static com.nalssilog.location.domain.QLocation.location;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.client.KakaoRegion;
import com.nalssilog.location.domain.Location;
import com.nalssilog.location.domain.LocationErrorCode;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서비스 호출용 Location 저장소.
 * 단순 조회는 Spring Data JPA에 위임하고, 키워드 검색은 QueryDSL로 처리한다.
 */
@Repository
@RequiredArgsConstructor
public class LocationRepository {

    private static final int SEARCH_LIMIT = 20;
    private static final String INSERT_IF_ABSENT = """
            insert Location (createdAt, updatedAt, adminCode, sido, sigungu, dong, latitude, longitude)
            values (:now, :now, :adminCode, :sido, :sigungu, :dong, :latitude, :longitude)
            on conflict (adminCode) do nothing
            """;

    private final LocationJpaRepository locationJpaRepository;
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    public List<LocationInfo> searchByKeyword(String keyword) {
        BooleanExpression matchesKeyword = location.sido.containsIgnoreCase(keyword)
                .or(location.sigungu.containsIgnoreCase(keyword))
                .or(location.dong.containsIgnoreCase(keyword))
                .or(location.sido.concat(" ")
                        .concat(location.sigungu).concat(" ")
                        .concat(location.dong)
                        .containsIgnoreCase(keyword));

        return queryFactory
                .selectFrom(location)
                .where(matchesKeyword)
                .orderBy(location.sido.asc(), location.sigungu.asc(), location.dong.asc())
                .limit(SEARCH_LIMIT)
                .fetch()
                .stream()
                .map(LocationInfo::of)
                .toList();
    }

    public LocationInfo getById(Long id) {
        return locationJpaRepository.findById(id)
                .map(LocationInfo::of)
                .orElseThrow(() -> new NalssiLogException(LocationErrorCode.LOCATION_NOT_FOUND));
    }

    /** 주어진 id 순서를 보존해 조회한다. (인기·즐겨찾기 목록의 정렬 유지용) */
    public List<LocationInfo> findByIds(List<Long> ids) {
        Map<Long, Location> byId = locationJpaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Location::getId, Function.identity()));

        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(LocationInfo::of)
                .toList();
    }

    /** 대표 지역을 admin_code 순서대로 조회한다(설정에 있으나 DB 에 없는 코드는 조용히 제외). */
    public List<LocationInfo> findByAdminCodes(List<String> adminCodes) {
        Map<String, Location> byCode = locationJpaRepository.findByAdminCodeIn(adminCodes).stream()
                .collect(Collectors.toMap(Location::getAdminCode, Function.identity()));

        return adminCodes.stream()
                .map(byCode::get)
                .filter(Objects::nonNull)
                .map(LocationInfo::of)
                .toList();
    }

    public boolean isEmpty() {
        return locationJpaRepository.count() == 0;
    }

    public void saveAll(List<Location> locations) {
        locationJpaRepository.saveAll(locations);
    }

    /**
     * 카카오 법정동 코드로 지역을 원자적으로 등록한 뒤 반환한다.
     * QueryDSL JPA가 INSERT를 지원하지 않아 Hibernate HQL upsert를 사용한다.
     */
    @Transactional
    public LocationInfo findOrCreate(KakaoRegion region) {
        Instant now = Instant.now();
        entityManager.unwrap(Session.class)
                .createMutationQuery(INSERT_IF_ABSENT)
                .setParameter("now", now)
                .setParameter("adminCode", region.adminCode())
                .setParameter("sido", region.sido())
                .setParameter("sigungu", region.sigungu())
                .setParameter("dong", region.dong())
                .setParameter("latitude", region.latitude())
                .setParameter("longitude", region.longitude())
                .executeUpdate();

        return locationJpaRepository.findByAdminCode(region.adminCode())
                .map(LocationInfo::of)
                .orElseThrow(() -> new NalssiLogException(LocationErrorCode.LOCATION_NOT_FOUND));
    }
}
