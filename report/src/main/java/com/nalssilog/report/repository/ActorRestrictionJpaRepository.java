package com.nalssilog.report.repository;

import com.nalssilog.report.domain.ActorRestriction;
import com.nalssilog.report.domain.ActorType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActorRestrictionJpaRepository extends JpaRepository<ActorRestriction, Long> {

    @Query("""
            select restriction
            from ActorRestriction restriction
            where restriction.actorType = :actorType
              and restriction.actorKey = :actorKey
              and restriction.liftedAt is null
              and (restriction.expiresAt is null or restriction.expiresAt > :now)
            order by restriction.createdAt desc
            """)
    List<ActorRestriction> findActive(
            @Param("actorType") ActorType actorType,
            @Param("actorKey") String actorKey,
            @Param("now") Instant now);
}
