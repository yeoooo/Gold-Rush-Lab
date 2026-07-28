package io.devyeoooo.Gold_Rush_Lab.mine.service;

import io.devyeoooo.Gold_Rush_Lab.observability.LockStrategy;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningFailureClassifier;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningFailureType;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.retry.MethodRetryEvent;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiningRetryServiceTest {

    private MiningMetrics miningMetrics;
    private MiningRetryService miningRetryService;

    @BeforeEach
    void setUp() {
        miningMetrics = mock(MiningMetrics.class);
        miningRetryService = new MiningRetryService(
                mock(MineService.class),
                miningMetrics,
                new MiningFailureClassifier()
        );
    }

    @Test
    void 재시도가_예정된_낙관적_락_실패를_기록한다() throws Exception {
        MethodRetryEvent event = retryEvent(
                MiningRetryService.class.getMethod("mine", UUID.class, Long.class),
                new OptimisticLockingFailureException("conflict"),
                false
        );

        miningRetryService.recordRetry(event);

        verify(miningMetrics).incrementOptimisticRetry();
    }

    @Test
    void 재시도를_소진한_낙관적_락_실패를_최종_실패로_기록한다() throws Exception {
        MethodRetryEvent event = retryEvent(
                MiningRetryService.class.getMethod("mine", UUID.class, Long.class),
                new OptimisticLockingFailureException("conflict"),
                true
        );

        miningRetryService.recordRetry(event);

        verify(miningMetrics).recordFailure(
                LockStrategy.OPTIMISTIC,
                MiningFailureType.OPTIMISTIC_LOCK
        );
        verify(miningMetrics, never()).incrementOptimisticRetry();
    }

    @Test
    void 다른_서비스의_재시도_이벤트는_무시한다() throws Exception {
        MethodRetryEvent event = retryEvent(
                MineService.class.getMethod("mine", UUID.class, Long.class),
                new OptimisticLockingFailureException("conflict"),
                false
        );

        miningRetryService.recordRetry(event);

        verify(miningMetrics, never()).incrementOptimisticRetry();
        verify(miningMetrics, never()).recordFailure(
                LockStrategy.OPTIMISTIC,
                MiningFailureType.OPTIMISTIC_LOCK
        );
    }

    private MethodRetryEvent retryEvent(Method method, Throwable failure, boolean retryAborted) {
        MethodRetryEvent event = mock(MethodRetryEvent.class);
        when(event.getMethod()).thenReturn(method);
        when(event.getFailure()).thenReturn(failure);
        when(event.isRetryAborted()).thenReturn(retryAborted);
        return event;
    }
}
