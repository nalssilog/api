package com.nalssilog.location.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class LocationInfoTest {

    @Test
    void createsFullAndShortUrbanLabels() {
        LocationInfo location = new LocationInfo(
                1L, "경기도", "수원시 영통구", "이의동", null, null);

        assertThat(location.label()).isEqualTo("경기도 수원시 영통구 이의동");
        assertThat(location.shortLabel()).isEqualTo("수원시 영통구 이의동");
    }

    @Test
    void omitsEmptyAdministrativeLevelWithoutLeavingExtraSpaces() {
        LocationInfo location = new LocationInfo(
                1L, "세종특별자치시", "", "조치원읍", null, null);

        assertThat(location.label()).isEqualTo("세종특별자치시 조치원읍");
        assertThat(location.shortLabel()).isEqualTo("조치원읍");
    }

    @Test
    void keepsRuralTownAndVillageTogetherInShortLabel() {
        LocationInfo location = new LocationInfo(
                1L, "충청북도", "괴산군", "괴산읍 동부리", null, null);

        assertThat(location.label()).isEqualTo("충청북도 괴산군 괴산읍 동부리");
        assertThat(location.shortLabel()).isEqualTo("괴산군 괴산읍 동부리");
    }
}
