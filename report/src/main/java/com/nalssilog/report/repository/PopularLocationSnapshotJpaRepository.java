package com.nalssilog.report.repository;

import com.nalssilog.report.domain.PopularLocationSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopularLocationSnapshotJpaRepository
        extends JpaRepository<PopularLocationSnapshot, Long> {

    Optional<PopularLocationSnapshot> findFirstByOrderByCalculatedAtDescIdDesc();
}
