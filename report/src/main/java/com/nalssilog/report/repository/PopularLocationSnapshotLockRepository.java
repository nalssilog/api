package com.nalssilog.report.repository;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.domain.PopularLocationSnapshotLock;
import com.nalssilog.report.domain.ReportErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PopularLocationSnapshotLockRepository {

    private static final Long LOCK_ID = 1L;

    private final EntityManager entityManager;

    public void acquire() {
        PopularLocationSnapshotLock lock = entityManager.find(
                PopularLocationSnapshotLock.class,
                LOCK_ID,
                LockModeType.PESSIMISTIC_WRITE);

        if (lock == null) {
            throw new NalssiLogException(
                    ReportErrorCode.POPULAR_SNAPSHOT_LOCK_UNAVAILABLE);
        }
    }
}
