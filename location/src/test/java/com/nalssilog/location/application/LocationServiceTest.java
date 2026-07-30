package com.nalssilog.location.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.application.dto.PopularLocationSnapshotData;
import com.nalssilog.location.client.KakaoMapClient;
import com.nalssilog.location.client.KakaoRegion;
import com.nalssilog.location.domain.LocationErrorCode;
import com.nalssilog.location.domain.PopularRankMovement;
import com.nalssilog.location.repository.LocationRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class LocationServiceTest {

    private final LocationRepository locationRepository = mock(LocationRepository.class);
    private final PopularLocationSource popularLocationSource = mock(PopularLocationSource.class);
    private final KakaoMapClient kakaoMapClient = mock(KakaoMapClient.class);
    private final LocationService service = new LocationService(
            locationRepository,
            popularLocationSource,
            kakaoMapClient
    );

    @Test
    void resolvesKakaoRegionAndReturnsPersistedLocation() {
        KakaoRegion region = new KakaoRegion(
                "1168010100", "서울특별시", "강남구", "역삼동", 37.500622, 127.036456);
        LocationInfo expected = new LocationInfo(
                1L, "서울특별시", "강남구", "역삼동", 37.500622, 127.036456);

        when(kakaoMapClient.reverseGeocode(37.5, 127.03)).thenReturn(region);
        when(locationRepository.findOrCreate(region)).thenReturn(expected);

        LocationInfo actual = service.reverseGeocode(37.5, 127.03);

        assertThat(actual).isSameAs(expected);
        verify(locationRepository).findOrCreate(region);
    }

    @Test
    void rejectsInvalidCoordinatesBeforeCallingKakao() {
        assertThatThrownBy(() -> service.reverseGeocode(91, 127))
                .isInstanceOfSatisfying(NalssiLogException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(LocationErrorCode.INVALID_COORDINATES));
        verifyNoInteractions(kakaoMapClient);
    }

    @Test
    void searchesJeonbukWithItsCurrentOfficialNameWhenLegacyNameIsEntered() {
        PageRequest pageable = PageRequest.of(0, 5);

        when(locationRepository.searchByKeyword("전북특별자치도 전주시", pageable))
                .thenReturn(Page.empty(pageable));

        service.search("전라북도 전주시", 0);

        verify(locationRepository).searchByKeyword("전북특별자치도 전주시", pageable);
    }

    @Test
    void searchesIntegratedJeonnamGwangjuWhenLegacyNameIsEntered() {
        PageRequest pageable = PageRequest.of(2, 5);

        when(locationRepository.searchByKeyword("전남광주통합특별시 순천시", pageable))
                .thenReturn(Page.empty(pageable));

        service.search("전라남도 순천시", 2);

        verify(locationRepository).searchByKeyword("전남광주통합특별시 순천시", pageable);
    }

    @Test
    void expandsAmbiguousLegacyJeollaNameToBothCurrentRegions() {
        PageRequest pageable = PageRequest.of(0, 5);

        when(locationRepository.searchByKeyword("전", pageable))
                .thenReturn(Page.empty(pageable));

        service.search("전라", 0);

        verify(locationRepository).searchByKeyword("전", pageable);
    }

    @Test
    void keepsFollowingTokensWhenLegacyJeollaNameIsEntered() {
        PageRequest pageable = PageRequest.of(0, 5);

        when(locationRepository.searchByKeyword("전 전주시", pageable))
                .thenReturn(Page.empty(pageable));

        service.search("전라도 전주시", 0);

        verify(locationRepository).searchByKeyword("전 전주시", pageable);
    }

    @Test
    void normalizesRepeatedWhitespaceBeforeSearching() {
        PageRequest pageable = PageRequest.of(0, 5);

        when(locationRepository.searchByKeyword("서울 강남구", pageable))
                .thenReturn(Page.empty(pageable));

        service.search("  서울   강남구  ", 0);

        verify(locationRepository).searchByKeyword("서울 강남구", pageable);
    }

    @Test
    void returnsFiveSearchItemsWithTotalPageMetadata() {
        List<LocationInfo> items = List.of(
                location(1L, "동1"),
                location(2L, "동2"),
                location(3L, "동3"),
                location(4L, "동4"),
                location(5L, "동5"));
        PageRequest pageable = PageRequest.of(1, 5);

        when(locationRepository.searchByKeyword("서", pageable))
                .thenReturn(new PageImpl<>(items, pageable, 12));

        var result = service.search("서", 1);

        assertThat(result.items()).containsExactlyElementsOf(items);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(12);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void enrichesPopularSnapshotWithoutChangingItsRanking() {
        Instant calculatedAt = Instant.parse("2026-07-30T06:00:00Z");
        Instant windowStartedAt = calculatedAt.minusSeconds(7 * 24 * 60 * 60);
        LocationInfo first = location(2L, "인기동1");
        LocationInfo second = location(1L, "인기동2");
        PopularLocationSnapshotData snapshot = new PopularLocationSnapshotData(
                31L,
                calculatedAt,
                windowStartedAt,
                calculatedAt,
                "UNIQUE_REPORTERS_V1",
                List.of(
                        new PopularLocationSnapshotData.Rank(
                                2L,
                                1,
                                3,
                                2,
                                PopularRankMovement.UP,
                                4,
                                7,
                                calculatedAt.minusSeconds(60)),
                        new PopularLocationSnapshotData.Rank(
                                1L,
                                2,
                                1,
                                -1,
                                PopularRankMovement.DOWN,
                                3,
                                5,
                                calculatedAt.minusSeconds(120))));

        when(popularLocationSource.latestSnapshot()).thenReturn(snapshot);
        when(locationRepository.findByIds(List.of(2L, 1L))).thenReturn(List.of(first, second));

        var result = service.getPopular();

        assertThat(result.snapshotId()).isEqualTo(31L);
        assertThat(result.items()).extracting(item -> item.location().id())
                .containsExactly(2L, 1L);
        assertThat(result.items()).extracting(item -> item.rank())
                .containsExactly(1, 2);
        assertThat(result.items().getFirst().movement()).isEqualTo(PopularRankMovement.UP);
        assertThat(result.items().getFirst().uniqueReporterCount()).isEqualTo(4);
    }

    private LocationInfo location(Long id, String dong) {

        return new LocationInfo(id, "서울특별시", "강남구", dong, 37.5, 127.0);
    }
}
