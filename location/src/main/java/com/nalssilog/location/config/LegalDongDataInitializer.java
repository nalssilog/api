package com.nalssilog.location.config;

import com.nalssilog.location.config.LegalDongCsvReader.LegalDongRow;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 버전에 해당하는 전국 법정동 데이터를 최초 한 번 적재한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegalDongDataInitializer implements ApplicationRunner {

    static final String DATASET_VERSION = "20260630";
    static final String DATASET_RESOURCE = "data/legal-dongs-" + DATASET_VERSION + ".csv";

    private static final int BATCH_SIZE = 1_000;
    private static final String UPSERT_SQL = """
            insert into location (
                created_at, updated_at, admin_code, sido, sigungu, dong, latitude, longitude
            ) values (
                current_timestamp, current_timestamp, ?, ?, ?, ?, null, null
            )
            on conflict (admin_code) do update set
                sido = excluded.sido,
                sigungu = excluded.sigungu,
                dong = excluded.dong,
                updated_at = current_timestamp
            where (location.sido, location.sigungu, location.dong)
                is distinct from (excluded.sido, excluded.sigungu, excluded.dong)
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 롤링 배포 중 두 인스턴스가 동시에 같은 버전을 적재하지 않도록 DB 트랜잭션 잠금을 사용한다.
        jdbcTemplate.execute("select pg_advisory_xact_lock(814733, 1)");

        if (isImported()) {
            return;
        }

        List<LegalDongRow> rows = LegalDongCsvReader.read(
                new ClassPathResource(DATASET_RESOURCE));

        jdbcTemplate.batchUpdate(UPSERT_SQL, rows, BATCH_SIZE, this::setParameters);
        jdbcTemplate.update("""
                insert into location_dataset_import (dataset_version, source_name, imported_at)
                values (?, ?, current_timestamp)
                on conflict (dataset_version) do nothing
                """, DATASET_VERSION, DATASET_RESOURCE);

        log.info("Imported legal-dong dataset version={} rows={}", DATASET_VERSION, rows.size());
    }

    private boolean isImported() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from location_dataset_import
                    where dataset_version = ?
                )
                """, Boolean.class, DATASET_VERSION));
    }

    private void setParameters(PreparedStatement statement, LegalDongRow row)
            throws SQLException {
        statement.setString(1, row.adminCode());
        statement.setString(2, row.sido());
        statement.setString(3, row.sigungu());
        statement.setString(4, row.dong());
    }
}
