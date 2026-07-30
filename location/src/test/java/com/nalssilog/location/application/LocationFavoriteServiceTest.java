package com.nalssilog.location.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.common.response.PageResponse;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.domain.LocationFavorite;
import com.nalssilog.location.repository.LocationFavoriteRepository;
import com.nalssilog.location.repository.LocationRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@SuppressWarnings("java:S5960")
class LocationFavoriteServiceTest {

    private final LocationFavoriteRepository favoriteRepository =
            mock(LocationFavoriteRepository.class);
    private final LocationRepository locationRepository =
            mock(LocationRepository.class);
    private final LocationFavoriteService service =
            new LocationFavoriteService(favoriteRepository, locationRepository);

    @Test
    void loadsFavoritesInFixedFiveItemPagesWithTotals() {
        List<Long> ids = List.of(11L, 12L);
        List<LocationInfo> locations = ids.stream()
                .map(id -> new LocationInfo(
                        id,
                        "서울특별시",
                        "강남구",
                        "동" + id,
                        null,
                        null))
                .toList();
        List<LocationFavorite> favorites = ids.stream()
                .map(id -> LocationFavorite.of(7L, id))
                .toList();
        PageRequest pageable = PageRequest.of(2, 5);

        when(favoriteRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(7L, pageable))
                .thenReturn(new PageImpl<>(favorites, pageable, 12));
        when(locationRepository.findByIds(ids)).thenReturn(locations);

        PageResponse<LocationInfo> result = service.listFavorites(7L, 2);

        assertThat(result.items()).containsExactlyElementsOf(locations);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(12);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isFalse();
        verify(favoriteRepository).findAllByMemberIdOrderByCreatedAtDescIdDesc(7L, pageable);
    }
}
