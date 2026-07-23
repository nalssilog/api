package com.nalssilog.location.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
}
