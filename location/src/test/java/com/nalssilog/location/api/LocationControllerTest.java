package com.nalssilog.location.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nalssilog.common.response.PageResponse;
import com.nalssilog.location.application.LocationService;
import com.nalssilog.location.application.dto.LocationInfo;
import com.nalssilog.location.application.dto.PopularLocationSnapshotInfo;
import com.nalssilog.location.domain.PopularRankMovement;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SuppressWarnings("java:S5960")
class LocationControllerTest {

    private final LocationService locationService = mock(LocationService.class);
    private final LocationController controller = new LocationController(locationService);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    @Test
    void returnsAtomicPopularSnapshotWithFiveItemDisplayMetadata() throws Exception {
        Instant calculatedAt = Instant.parse("2026-07-30T06:00:00Z");
        Instant windowStartedAt = calculatedAt.minusSeconds(7 * 24 * 60 * 60);
        LocationInfo location = new LocationInfo(
                1L, "서울특별시", "강남구", "역삼동", 37.5, 127.0);
        PopularLocationSnapshotInfo snapshot = new PopularLocationSnapshotInfo(
                31L,
                calculatedAt,
                windowStartedAt,
                calculatedAt,
                "UNIQUE_REPORTERS_V1",
                List.of(new PopularLocationSnapshotInfo.Item(
                        1,
                        3,
                        2,
                        PopularRankMovement.UP,
                        4,
                        7,
                        calculatedAt.minusSeconds(60),
                        location)));

        when(locationService.getPopular()).thenReturn(snapshot);

        mockMvc.perform(get("/api/locations/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotId").value("31"))
                .andExpect(jsonPath("$.algorithmVersion").value("UNIQUE_REPORTERS_V1"))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].rank").value(1))
                .andExpect(jsonPath("$.items[0].previousRank").value(3))
                .andExpect(jsonPath("$.items[0].rankChange").value(2))
                .andExpect(jsonPath("$.items[0].movement").value("UP"))
                .andExpect(jsonPath("$.items[0].uniqueReporterCount").value(4))
                .andExpect(jsonPath("$.items[0].reportCount").value(7))
                .andExpect(jsonPath("$.items[0].location.id").value("1"));
        verify(locationService).getPopular();
    }

    @Test
    void usesZeroAsDefaultSearchPageAndReturnsTotalMetadata() throws Exception {
        when(locationService.search("서", 0))
                .thenReturn(PageResponse.of(List.of(), 0, 5, 0));

        mockMvc.perform(get("/api/locations").param("keyword", "서"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.hasPrevious").value(false))
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(locationService).search("서", 0);
    }

    @Test
    void rejectsNegativeSearchPage() throws Exception {
        mockMvc.perform(get("/api/locations")
                        .param("keyword", "서")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void rejectsOversizedSearchKeyword() throws Exception {
        mockMvc.perform(get("/api/locations")
                        .param("keyword", "가".repeat(51)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }
}
