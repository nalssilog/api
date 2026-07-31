package com.nalssilog.location.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.common.response.PageResponse;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.domain.LocationErrorCode;
import com.nalssilog.location.domain.LocationFavorite;
import com.nalssilog.location.repository.LocationFavoriteRepository;
import com.nalssilog.location.repository.LocationRepository;
import java.sql.SQLException;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
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
    void existingFavoriteIsIdempotent() {
        when(favoriteRepository.existsByMemberIdAndLocationId(7L, 11L))
                .thenReturn(true);

        service.addFavorite(7L, 11L);

        verify(locationRepository).getById(11L);
        verify(favoriteRepository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentFavoriteCollisionBecomesDomainConflict() {
        DataIntegrityViolationException collision = new DataIntegrityViolationException(
                "duplicate favorite",
                new ConstraintViolationException(
                        "duplicate favorite",
                        new SQLException(),
                        "uk_location_favorite_member_location"));

        when(favoriteRepository.existsByMemberIdAndLocationId(7L, 11L))
                .thenReturn(false);
        when(favoriteRepository.saveAndFlush(any(LocationFavorite.class)))
                .thenThrow(collision);

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.addFavorite(7L, 11L));

        assertThat(exception.getErrorCode())
                .isEqualTo(LocationErrorCode.FAVORITE_ALREADY_EXISTS);
    }

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
