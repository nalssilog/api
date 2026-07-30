package com.nalssilog.location.repository;

import static com.nalssilog.location.domain.QLocation.location;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.client.KakaoRegion;
import com.nalssilog.location.domain.Location;
import com.nalssilog.location.domain.LocationErrorCode;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 서비스 호출용 Location 저장소.
 * 단순 조회는 Spring Data JPA에 위임하고, 키워드 검색은 QueryDSL로 처리한다.
 */
@Repository
@RequiredArgsConstructor
public class LocationRepository {

    private final LocationJpaRepository locationJpaRepository;
    private final JPAQueryFactory queryFactory;

    public Page<LocationInfo> searchByKeyword(String keyword, Pageable pageable) {
        List<String> tokens = Arrays.stream(keyword.split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();

        if (tokens.isEmpty()) {

            return Page.empty(pageable);
        }

        StringExpression label = location.sido.concat(" ")
                .concat(location.sigungu).concat(" ")
                .concat(location.dong);
        BooleanBuilder matchesKeyword = new BooleanBuilder();

        tokens.forEach(token -> matchesKeyword.and(matchesToken(label, token)));

        String firstToken = tokens.getFirst();
        NumberExpression<Integer> relevance = new CaseBuilder()
                .when(location.sido.eq(keyword)
                        .or(location.sigungu.eq(keyword))
                        .or(location.dong.eq(keyword))
                        .or(label.eq(keyword)))
                .then(0)
                .when(location.sido.startsWith(firstToken))
                .then(1)
                .when(location.sigungu.startsWith(firstToken))
                .then(2)
                .when(location.dong.startsWith(firstToken))
                .then(3)
                .when(label.startsWith(keyword))
                .then(4)
                .otherwise(5);

        List<LocationInfo> items = queryFactory
                .selectFrom(location)
                .where(matchesKeyword)
                .orderBy(
                        relevance.asc(),
                        location.sido.asc(),
                        location.sigungu.asc(),
                        location.dong.asc(),
                        location.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(LocationInfo::of)
                .toList();
        Long totalElements = queryFactory
                .select(location.count())
                .from(location)
                .where(matchesKeyword)
                .fetchOne();

        return new PageImpl<>(
                items,
                pageable,
                totalElements == null ? 0 : totalElements);
    }

    private BooleanExpression matchesToken(StringExpression label, String token) {
        BooleanExpression prefixMatch = location.sido.startsWith(token)
                .or(location.sigungu.startsWith(token))
                .or(location.dong.startsWith(token))
                .or(label.startsWith(token));

        if (token.length() == 1) {

            return prefixMatch;
        }

        return prefixMatch.or(label.contains(token));
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

    public boolean isEmpty() {

        return locationJpaRepository.count() == 0;
    }

    public void saveAll(List<Location> locations) {
        locationJpaRepository.saveAll(locations);
    }

    /**
     * 카카오 법정동 코드를 먼저 조회하고 없으면 JPA로 등록한다.
     * 동시에 같은 코드가 등록되면 유니크 키가 승자를 정하고 커밋된 행을 다시 조회한다.
     */
    public LocationInfo findOrCreate(KakaoRegion region) {

        return locationJpaRepository.findByAdminCode(region.adminCode())
                .map(LocationInfo::of)
                .orElseGet(() -> saveOrLoadConcurrent(region));
    }

    private LocationInfo saveOrLoadConcurrent(KakaoRegion region) {
        try {
            Location location = Location.of(
                    region.adminCode(),
                    region.sido(),
                    region.sigungu(),
                    region.dong(),
                    region.latitude(),
                    region.longitude());

            return LocationInfo.of(locationJpaRepository.saveAndFlush(location));
        } catch (DataIntegrityViolationException exception) {

            return findByAdminCodeOrThrow(region.adminCode());
        }
    }

    private LocationInfo findByAdminCodeOrThrow(String adminCode) {

        return locationJpaRepository.findByAdminCode(adminCode)
                .map(LocationInfo::of)
                .orElseThrow(() -> new NalssiLogException(LocationErrorCode.LOCATION_NOT_FOUND));
    }
}
