package com.nalssilog.report.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.domain.PopularLocationSnapshotLock;
import com.nalssilog.report.domain.ReportErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960")
class PopularLocationSnapshotLockRepositoryTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final PopularLocationSnapshotLockRepository repository =
            new PopularLocationSnapshotLockRepository(entityManager);

    @Test
    void acquiresDatabasePessimisticWriteLock() {
        PopularLocationSnapshotLock lock =
                mock(PopularLocationSnapshotLock.class);

        when(entityManager.find(
                PopularLocationSnapshotLock.class,
                1L,
                LockModeType.PESSIMISTIC_WRITE)).thenReturn(lock);

        repository.acquire();

        verify(entityManager).find(
                PopularLocationSnapshotLock.class,
                1L,
                LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void failsWithDomainErrorWhenLockRowIsMissing() {
        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                repository::acquire);

        assertThat(exception.getErrorCode())
                .isEqualTo(ReportErrorCode.POPULAR_SNAPSHOT_LOCK_UNAVAILABLE);
    }
}
