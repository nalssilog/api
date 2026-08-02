package com.nalssilog.report.repository;

import com.nalssilog.report.domain.ActorBlock;
import com.nalssilog.report.domain.ActorType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActorBlockJpaRepository extends JpaRepository<ActorBlock, Long> {

    Optional<ActorBlock> findByBlockerTypeAndBlockerKeyAndBlockedTypeAndBlockedKey(
            ActorType blockerType, String blockerKey, ActorType blockedType, String blockedKey);

    long countByBlockerTypeAndBlockerKey(ActorType blockerType, String blockerKey);

    Page<ActorBlock> findAllByBlockerTypeAndBlockerKeyOrderByCreatedAtDesc(
            ActorType blockerType, String blockerKey, Pageable pageable);
}
