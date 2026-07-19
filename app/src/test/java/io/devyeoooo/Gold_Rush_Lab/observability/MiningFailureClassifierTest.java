package io.devyeoooo.Gold_Rush_Lab.observability;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiningFailureClassifierTest {

    private final MiningFailureClassifier classifier = new MiningFailureClassifier();

    @Test
    void 락_획득_실패를_분류한다() {
        RuntimeException wrapped = new RuntimeException(
                new CannotAcquireLockException("lock acquisition failed")
        );

        assertEquals(MiningFailureType.CANNOT_ACQUIRE_LOCK, classifier.classify(wrapped));
    }

    @Test
    void 래핑된_JPA_락_타임아웃을_분류한다() {
        RuntimeException wrapped = new RuntimeException(
                "outer",
                new RuntimeException("middle", new LockTimeoutException("timed out"))
        );

        assertEquals(MiningFailureType.LOCK_TIMEOUT, classifier.classify(wrapped));
    }

    @Test
    void PostgreSQL_SQLSTATE로_데드락을_가장_먼저_분류한다() {
        SQLException sqlException = new SQLException("deadlock detected", "40P01");
        CannotAcquireLockException wrapped = new CannotAcquireLockException(
                "translated exception",
                sqlException
        );

        assertEquals(MiningFailureType.DEADLOCK, classifier.classify(wrapped));
    }

    @Test
    void 래핑된_낙관적_락_충돌을_분류한다() {
        RuntimeException wrapped = new RuntimeException(new OptimisticLockException("conflict"));

        assertEquals(MiningFailureType.OPTIMISTIC_LOCK, classifier.classify(wrapped));
    }

    @Test
    void 알_수_없는_예외를_unknown으로_분류한다() {
        assertEquals(MiningFailureType.UNKNOWN, classifier.classify(new RuntimeException("boom")));
    }
}
