package com.nalssilog.location.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;

/**
 * 국토교통부 전국 법정동 CSV를 서비스의 검색 단위로 정규화한다.
 *
 * <p>시도/시군구 행은 제외한다. 리가 존재하는 읍면은 읍면 자체 행을 제외하고
 * 실제 최하위 법정리만 {@code "읍면 리"} 형태로 사용한다.</p>
 */
final class LegalDongCsvReader {

    private static final String EXPECTED_HEADER =
            "법정동코드,시도명,시군구명,읍면동명,리명,순위,생성일자";

    private LegalDongCsvReader() {
    }

    static List<LegalDongRow> read(Resource resource) {
        List<SourceRow> sourceRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String header = removeBom(reader.readLine());
            if (!EXPECTED_HEADER.equals(header)) {
                throw new IllegalStateException("Unexpected legal-dong CSV header: " + header);
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!line.isBlank()) {
                    sourceRows.add(parse(line, lineNumber));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read legal-dong CSV: " + resource, e);
        }

        Set<String> parentCodesWithRi = new HashSet<>();
        for (SourceRow row : sourceRows) {
            if (!row.ri().isBlank()) {
                parentCodesWithRi.add(row.adminCode().substring(0, 8) + "00");
            }
        }

        return sourceRows.stream()
                .filter(row -> !row.eupMyeonDong().isBlank())
                .filter(row -> !parentCodesWithRi.contains(row.adminCode()))
                .map(SourceRow::toLegalDongRow)
                .toList();
    }

    private static SourceRow parse(String line, int lineNumber) {
        String[] columns = line.split(",", -1);
        if (columns.length != 7) {
            throw new IllegalStateException(
                    "Invalid legal-dong CSV column count at line " + lineNumber);
        }

        String adminCode = columns[0].strip();
        String sido = columns[1].strip();
        if (!adminCode.matches("\\d{10}") || sido.isBlank()) {
            throw new IllegalStateException("Invalid legal-dong CSV row at line " + lineNumber);
        }

        return new SourceRow(
                adminCode,
                sido,
                normalizeSigungu(columns[2]),
                columns[3].strip(),
                columns[4].strip()
        );
    }

    /**
     * 공공데이터의 통합시 행정구 명칭(예: 성남시분당구)을
     * 카카오 응답 및 사용자 표기와 같은 형태(성남시 분당구)로 맞춘다.
     */
    private static String normalizeSigungu(String value) {
        String sigungu = value.strip();
        int citySuffixIndex = sigungu.indexOf('시');

        if (citySuffixIndex > 0
                && citySuffixIndex < sigungu.length() - 1
                && sigungu.endsWith("구")) {
            return sigungu.substring(0, citySuffixIndex + 1)
                    + " "
                    + sigungu.substring(citySuffixIndex + 1);
        }

        return sigungu;
    }

    private static String removeBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    record LegalDongRow(String adminCode, String sido, String sigungu, String dong) {
    }

    private record SourceRow(
            String adminCode,
            String sido,
            String sigungu,
            String eupMyeonDong,
            String ri
    ) {

        LegalDongRow toLegalDongRow() {
            String dong = ri.isBlank() ? eupMyeonDong : eupMyeonDong + " " + ri;
            return new LegalDongRow(adminCode, sido, sigungu, dong);
        }
    }
}
