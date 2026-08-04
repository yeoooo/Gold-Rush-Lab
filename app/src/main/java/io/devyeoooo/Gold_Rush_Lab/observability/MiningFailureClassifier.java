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

    /**
     * 동기 채굴 중 발생한 예외를 메트릭용 실패 유형으로 분류하는 함수.
     *
     * 1. 최상위 예외부터 마지막 원인까지 예외 체인을 수집한다.
     * 2. deadlock과 timeout 여부를 순서대로 확인한다.
     * 3. 비관적 lock 획득 실패와 optimistic lock 충돌 여부를 확인한다.
     * 4. 어떤 조건에도 해당하지 않으면 UNKNOWN을 반환한다.
     *
     * @param throwable 분류할 최상위 예외
     * @return 원인 체인에서 확인된 실패 유형 또는 {@link MiningFailureType#UNKNOWN}
     */
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

    /**
     * 최상위 예외부터 모든 원인 예외를 수집하는 함수.
     *
     * 1. 객체 identity를 기준으로 예외를 집합에 추가한다.
     * 2. 원인 예외가 없거나 이미 방문한 예외를 만나면 탐색을 종료한다.
     *
     * @param throwable 원인 탐색을 시작할 예외
     * @return 최상위 예외를 포함한 원인 예외 집합
     */
    private Set<Throwable> causesOf(Throwable throwable) {
        Set<Throwable> causes = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && causes.add(current)) {
            current = current.getCause();
        }
        return causes;
    }

    /**
     * 원인 예외 중 지정한 타입과 호환되는 예외가 있는지 확인하는 함수.
     *
     * 1. 원인 예외를 순회하며 지정한 타입의 인스턴스인지 검사한다.
     * 2. 하나라도 일치하면 true를 반환한다.
     *
     * @param causes 검사할 원인 예외 집합
     * @param type 찾을 예외 타입
     * @return 지정한 타입과 호환되는 예외가 있으면 {@code true}
     */
    private boolean containsType(Set<Throwable> causes, Class<? extends Throwable> type) {
        return causes.stream().anyMatch(type::isInstance);
    }

    /**
     * 원인 예외의 타입 계층에 지정한 클래스 이름이 있는지 확인하는 함수.
     *
     * 1. 각 원인 예외의 실제 클래스를 가져온다.
     * 2. 실제 클래스와 상위 클래스의 단순 이름을 검사한다.
     * 3. 하나라도 일치하면 true를 반환한다.
     *
     * @param causes 검사할 원인 예외 집합
     * @param simpleName 찾을 예외 클래스의 단순 이름
     * @return 해당 이름을 가진 타입 계층의 예외가 있으면 {@code true}
     */
    private boolean containsClassName(Set<Throwable> causes, String simpleName) {
        return causes.stream().anyMatch(cause -> hasClassName(cause.getClass(), simpleName));
    }

    /**
     * 클래스의 상속 계층에서 지정한 단순 이름을 찾는 함수.
     *
     * 1. 현재 클래스의 단순 이름을 비교한다.
     * 2. 일치하지 않으면 상위 클래스로 이동해 반복한다.
     * 3. 일치하는 클래스가 없으면 false를 반환한다.
     *
     * @param type 탐색을 시작할 클래스
     * @param simpleName 찾을 단순 클래스 이름
     * @return 타입 계층에 같은 이름의 클래스가 있으면 {@code true}
     */
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

    /**
     * 원인 예외 중 지정한 SQLSTATE를 가진 SQL 예외가 있는지 확인하는 함수.
     *
     * 1. 원인 예외에서 SQLException만 선택한다.
     * 2. 각 SQLException의 SQLSTATE를 지정한 값과 비교한다.
     * 3. 하나라도 일치하면 true를 반환한다.
     *
     * @param causes 검사할 원인 예외 집합
     * @param sqlState 찾을 SQLSTATE
     * @return 일치하는 SQLSTATE가 있으면 {@code true}
     */
    private boolean containsSqlState(Set<Throwable> causes, String sqlState) {
        return causes.stream()
                .filter(SQLException.class::isInstance)
                .map(SQLException.class::cast)
                .anyMatch(exception -> sqlState.equals(exception.getSQLState()));
    }
}
