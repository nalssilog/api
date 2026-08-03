package com.nalssilog.report.repository;

import com.nalssilog.report.domain.ModerationAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationActionJpaRepository extends JpaRepository<ModerationAction, Long> {

    Page<ModerationAction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
