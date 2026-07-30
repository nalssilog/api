package com.nalssilog.report.repository;

import com.nalssilog.report.domain.PopularLocationRank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopularLocationRankJpaRepository
        extends JpaRepository<PopularLocationRank, Long> {

    List<PopularLocationRank> findAllBySnapshotIdOrderByPositionAsc(Long snapshotId);
}
