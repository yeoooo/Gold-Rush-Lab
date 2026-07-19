package io.devyeoooo.Gold_Rush_Lab.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class MiningMetrics {

    private static final String STRATEGY_TAG = "strategy";
    private static final String EXCEPTION_TAG = "exception";

    private final MeterRegistry meterRegistry;

    public MiningMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer.Sample startLockWait() {
        return Timer.start(meterRegistry);
    }

    /**
     * 애플리케이션에서 관찰한 락 획득 호출 지연 시간을 기록한다.
     * 실제 row lock 대기 시간 외에도 DB 네트워크 왕복과 SQL 실행 시간이 포함될 수 있다.
     */
    public void stopLockWait(Timer.Sample sample, LockStrategy strategy) {
        sample.stop(Timer.builder("gold.rush.mining.lock.wait")
                .description("Application-observed lock acquisition latency")
                .tag(STRATEGY_TAG, strategy.tagValue())
                .publishPercentileHistogram()
                .register(meterRegistry));
    }

    public void incrementMiningSuccess(LockStrategy strategy) {
        counter("gold.rush.mining.success", strategy).increment();
    }

    public void incrementMiningFailure(LockStrategy strategy, MiningFailureType failureType) {
        Counter.builder("gold.rush.mining.failure")
                .description("Number of failed mining operations")
                .tag(STRATEGY_TAG, strategy.tagValue())
                .tag(EXCEPTION_TAG, failureType.tagValue())
                .register(meterRegistry)
                .increment();
    }

    public void incrementLockTimeout(LockStrategy strategy) {
        counter("gold.rush.lock.timeout", strategy).increment();
    }

    public void incrementDeadlock(LockStrategy strategy) {
        counter("gold.rush.deadlock", strategy).increment();
    }

    public void incrementOptimisticRetry() {
        counter("gold.rush.optimistic.lock.retry", LockStrategy.OPTIMISTIC).increment();
    }

    public void recordFailure(LockStrategy strategy, MiningFailureType failureType) {
        incrementMiningFailure(strategy, failureType);
        if (failureType == MiningFailureType.DEADLOCK) {
            incrementDeadlock(strategy);
        } else if (failureType.isTimeout()) {
            incrementLockTimeout(strategy);
        }
    }

    private Counter counter(String name, LockStrategy strategy) {
        return Counter.builder(name)
                .tag(STRATEGY_TAG, strategy.tagValue())
                .register(meterRegistry);
    }
}
