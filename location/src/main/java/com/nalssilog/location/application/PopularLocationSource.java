package com.nalssilog.location.application;

import com.nalssilog.location.application.dto.PopularLocationSnapshotData;

/**
 * 인기 지역 랭킹의 공급자(port). 실제 집계와 스냅샷 저장은 report 모듈이 구현한다.
 */
public interface PopularLocationSource {

    PopularLocationSnapshotData latestSnapshot();
}
