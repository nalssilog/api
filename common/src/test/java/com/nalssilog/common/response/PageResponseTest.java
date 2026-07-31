package com.nalssilog.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960")
class PageResponseTest {

    @Test
    void calculatesNumberedPageMetadataFromTotalElements() {
        PageResponse<String> page = PageResponse.of(
                List.of("여섯", "일곱", "여덟", "아홉", "열"),
                1,
                5,
                12);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(12);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void returnsZeroPagesForAnEmptyResult() {
        PageResponse<String> page = PageResponse.of(List.of(), 0, 5, 0);

        assertThat(page.totalPages()).isZero();
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.hasNext()).isFalse();
    }
}
