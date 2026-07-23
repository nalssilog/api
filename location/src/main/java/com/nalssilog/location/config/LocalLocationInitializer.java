package com.nalssilog.location.config;

import com.nalssilog.location.domain.Location;
import com.nalssilog.location.repository.LocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발용 지역 시드. 실제 행정동(약 2만 건)은 운영 시 공공데이터로 적재하고, 여기선 검색 테스트용 소수만 넣는다.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalLocationInitializer implements ApplicationRunner {

    private static final String SEOUL = "서울특별시";

    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!locationRepository.isEmpty()) {
            return;
        }

        locationRepository.saveAll(List.of(
                Location.of("1168010100", SEOUL, "강남구", "역삼동", 37.500622, 127.036456),
                Location.of("1168010500", SEOUL, "강남구", "삼성동", 37.514322, 127.056819),
                Location.of("1165010800", SEOUL, "서초구", "서초동", 37.491906, 127.007829),
                Location.of("1156013000", SEOUL, "영등포구", "여의도동", 37.521624, 126.924191),
                Location.of("4113565000", "경기도", "성남시 분당구", "삼평동", 37.402238, 127.108667),
                Location.of("2611051000", "부산광역시", "중구", "남포동", 35.097442, 129.032297)
        ));
    }
}
