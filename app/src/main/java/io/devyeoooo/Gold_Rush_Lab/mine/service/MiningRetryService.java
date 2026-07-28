package io.devyeoooo.Gold_Rush_Lab.mine.service;

import io.devyeoooo.Gold_Rush_Lab.observability.LockStrategy;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningFailureClassifier;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningFailureType;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.resilience.retry.MethodRetryEvent;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MiningRetryService {

    private final MineService mineService;
    private final MiningMetrics miningMetrics;
    private final MiningFailureClassifier miningFailureClassifier;

    @Retryable(
            includes = OptimisticLockingFailureException.class,
            maxRetries = 20L,
            delay = 5,
            multiplier = 1.5,
            maxDelay = 100
    )
    public void mine(UUID sessionId, Long amount) {
        mineService.mine(sessionId, amount);
    }

    @EventListener
    public void recordRetry(MethodRetryEvent event) {
        if (event.getMethod().getDeclaringClass() != MiningRetryService.class) {
            return;
        }

        if (event.isRetryAborted()) {
            MiningFailureType failureType = miningFailureClassifier.classify(event.getFailure());
            if (failureType == MiningFailureType.OPTIMISTIC_LOCK) {
                miningMetrics.recordFailure(LockStrategy.OPTIMISTIC, failureType);
            }
            return;
        }

        miningMetrics.incrementOptimisticRetry();
    }
}
