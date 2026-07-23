package com.nalssilog.location.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalssilog.location.config.LegalDongCsvReader.LegalDongRow;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LegalDongCsvReaderTest {

    @Test
    void readsOnlySearchableLeafLegalDongs() {
        List<LegalDongRow> rows = LegalDongCsvReader.read(
                new ClassPathResource(LegalDongDataInitializer.DATASET_RESOURCE));

        assertThat(rows)
                .hasSize(18_864)
                .contains(new LegalDongRow("1168010100", "서울특별시", "강남구", "역삼동"))
                .contains(new LegalDongRow("4155040021", "경기도", "안성시", "죽산면 죽산리"))
                .contains(new LegalDongRow(
                        "4113510900", "경기도", "성남시 분당구", "삼평동"))
                .doesNotContain(new LegalDongRow("4155040000", "경기도", "안성시", "죽산면"));
    }
}
