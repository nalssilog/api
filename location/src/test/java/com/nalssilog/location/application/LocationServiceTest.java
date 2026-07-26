package com.nalssilog.location.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.client.KakaoMapClient;
import com.nalssilog.location.client.KakaoRegion;
import com.nalssilog.location.config.LocationProperties;
import com.nalssilog.location.domain.LocationErrorCode;
import com.nalssilog.location.repository.LocationRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class LocationServiceTest {

    private final LocationRepository locationRepository = mock(LocationRepository.class);
    private final PopularLocationSource popularLocationSource = mock(PopularLocationSource.class);
    private final KakaoMapClient kakaoMapClient = mock(KakaoMapClient.class);
    private final LocationService service = new LocationService(
            locationRepository,
            popularLocationSource,
            new LocationProperties(List.of()),
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
        service.search("전라북도 전주시");

        verify(locationRepository).searchByKeyword("전북특별자치도 전주시");
    }

    @Test
    void searchesIntegratedJeonnamGwangjuWhenLegacyNameIsEntered() {
        service.search("전라남도 순천시");

        verify(locationRepository).searchByKeyword("전남광주통합특별시 순천시");
    }

    @Test
    void fillsPopularLocationsToFiveWithNonDuplicateFeaturedLocations() {
        List<String> featuredCodes = List.of("code-2", "code-3", "code-4", "code-5", "code-6");
        LocationService popularService = new LocationService(
                locationRepository,
                popularLocationSource,
                new LocationProperties(featuredCodes),
                kakaoMapClient
        );
        LocationInfo popularOne = location(1L, "인기동1");
        LocationInfo popularTwo = location(2L, "인기동2");
        LocationInfo featuredThree = location(3L, "대표동3");
        LocationInfo featuredFour = location(4L, "대표동4");
        LocationInfo featuredFive = location(5L, "대표동5");
        LocationInfo featuredSix = location(6L, "대표동6");

        when(popularLocationSource.topLocationIds(5)).thenReturn(List.of(1L, 2L));
        when(locationRepository.findByIds(List.of(1L, 2L))).thenReturn(List.of(popularOne, popularTwo));
        when(locationRepository.findByAdminCodes(featuredCodes)).thenReturn(List.of(
                popularTwo, featuredThree, featuredFour, featuredFive, featuredSix));

        List<LocationInfo> result = popularService.getPopular();

        assertThat(result).containsExactly(
                popularOne, popularTwo, featuredThree, featuredFour, featuredFive);
    }

    @Test
    void doesNotLoadFeaturedLocationsWhenFivePopularLocationsExist() {
        List<Long> popularIds = List.of(1L, 2L, 3L, 4L, 5L);
        List<LocationInfo> popular = List.of(
                location(1L, "인기동1"),
                location(2L, "인기동2"),
                location(3L, "인기동3"),
                location(4L, "인기동4"),
                location(5L, "인기동5")
        );
        when(popularLocationSource.topLocationIds(5)).thenReturn(popularIds);
        when(locationRepository.findByIds(popularIds)).thenReturn(popular);

        assertThat(service.getPopular()).containsExactlyElementsOf(popular);
        verify(locationRepository, never()).findByAdminCodes(List.of());
    }

    private LocationInfo location(Long id, String dong) {
        return new LocationInfo(id, "서울특별시", "강남구", dong, 37.5, 127.0);
    }
}
