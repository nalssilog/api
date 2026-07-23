package com.nalssilog.location.application;

import java.util.List;

/**
 * 인기 지역 랭킹의 공급자(port). 실제 데이터는 제보 활동에 기반하므로 report 모듈이 구현(adapter)한다.
 * location 은 report 를 직접 의존하지 못하므로(의존 역방향), 이 인터페이스를 통해 역전시켜 받는다.
 * 구현은 최근 활동이 많은 순서로 locationId 를 돌려주고, 데이터가 없으면 빈 목록을 반환한다.
 */
public interface PopularLocationSource {

    List<Long> topLocationIds(int size);
}
