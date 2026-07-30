package com.nalssilog.location.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.common.response.PageResponse;
import com.nalssilog.location.application.LocationFavoriteService;
import com.nalssilog.location.application.dto.LocationInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960")
class LocationFavoriteControllerTest {

    private final LocationFavoriteService favoriteService =
            mock(LocationFavoriteService.class);
    private final LocationFavoriteController controller =
            new LocationFavoriteController(favoriteService);

    @Test
    void preservesFavoritePageTotalsWhileMappingLocations() {
        LocationInfo location = new LocationInfo(
                1L,
                "서울특별시",
                "강남구",
                "역삼동",
                37.5,
                127.0);

        when(favoriteService.listFavorites(7L, 1))
                .thenReturn(PageResponse.of(List.of(location), 1, 5, 8));

        var result = controller.myFavorites(7L, 1);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(8);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.items()).singleElement()
                .satisfies(item -> assertThat(item.id()).isEqualTo("1"));
        verify(favoriteService).listFavorites(7L, 1);
    }
}
