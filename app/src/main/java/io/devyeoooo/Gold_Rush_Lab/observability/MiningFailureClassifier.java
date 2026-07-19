package io.devyeoooo.Gold_Rush_Lab.observability;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.QueryTimeoutException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Component
public class MiningFailureClassifier {

    private static final String POSTGRES_DEADLOCK_SQL_STATE = "40P01";

    public MiningFailureType classify(Throwable throwable) {
        Set<Throwable> causes = causesOf(throwable);

        if (containsSqlState(causes, POSTGRES_DEADLOCK_SQL_STATE)
                || containsClassName(causes, "DeadlockLoserDataAccessException")) {
            return MiningFailureType.DEADLOCK;
        }
        if (containsType(causes, LockTimeoutException.class)
                || containsType(causes, QueryTimeoutException.class)
                || containsType(causes, org.springframework.dao.QueryTimeoutException.class)) {
            return MiningFailureType.LOCK_TIMEOUT;
        }
        if (containsType(causes, CannotAcquireLockException.class)
                || containsType(causes, PessimisticLockingFailureException.class)) {
            return MiningFailureType.CANNOT_ACQUIRE_LOCK;
        }
        if (containsType(causes, OptimisticLockException.class)
                || containsType(causes, OptimisticLockingFailureException.class)) {
            return MiningFailureType.OPTIMISTIC_LOCK;
        }
        return MiningFailureType.UNKNOWN;
    }

    private Set<Throwable> causesOf(Throwable throwable) {
        Set<Throwable> causes = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && causes.add(current)) {
            current = current.getCause();
        }
        return causes;
    }

    private boolean containsType(Set<Throwable> causes, Class<? extends Throwable> type) {
        return causes.stream().anyMatch(type::isInstance);
    }

    private boolean containsClassName(Set<Throwable> causes, String simpleName) {
        return causes.stream().anyMatch(cause -> hasClassName(cause.getClass(), simpleName));
    }

    private boolean hasClassName(Class<?> type, String simpleName) {
        Class<?> current = type;
        while (current != null) {
            if (simpleName.equals(current.getSimpleName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private boolean containsSqlState(Set<Throwable> causes, String sqlState) {
        return causes.stream()
                .filter(SQLException.class::isInstance)
                .map(SQLException.class::cast)
                .anyMatch(exception -> sqlState.equals(exception.getSQLState()));
    }
}
