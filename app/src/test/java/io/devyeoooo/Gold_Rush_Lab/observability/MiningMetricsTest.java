package io.devyeoooo.Gold_Rush_Lab.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningMetricsTest {

    private SimpleMeterRegistry registry;
    private MiningMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new MiningMetrics(registry);
    }

    @Test
    void 성공과_실패를_제한된_태그로_기록한다() {
        metrics.incrementMiningSuccess(LockStrategy.PESSIMISTIC);
        metrics.incrementMiningFailure(LockStrategy.PESSIMISTIC, MiningFailureType.LOCK_TIMEOUT);

        assertEquals(1.0, registry.get("gold.rush.mining.success")
                .tag("strategy", "pessimistic").counter().count());
        assertEquals(1.0, registry.get("gold.rush.mining.failure")
                .tag("strategy", "pessimistic")
                .tag("exception", "lock_timeout")
                .counter().count());
    }

    @Test
    void 타임아웃과_데드락을_실패와_함께_기록하되_중복_집계하지_않는다() {
        metrics.recordFailure(LockStrategy.PESSIMISTIC, MiningFailureType.LOCK_TIMEOUT);
        metrics.recordFailure(LockStrategy.PESSIMISTIC, MiningFailureType.DEADLOCK);

        assertEquals(1.0, registry.get("gold.rush.lock.timeout").counter().count());
        assertEquals(1.0, registry.get("gold.rush.deadlock").counter().count());
        assertEquals(2, registry.find("gold.rush.mining.failure").counters().size());
    }

    @Test
    void 락_획득_호출_지연_시간을_기록한다() {
        Timer.Sample sample = metrics.startLockWait();
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));

        metrics.stopLockWait(sample, LockStrategy.PESSIMISTIC);

        Timer timer = registry.get("gold.rush.mining.lock.wait")
                .tag("strategy", "pessimistic")
                .timer();
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) > 0);
    }

    @Test
    void 실제_낙관적_락_재시도_카운터를_증가시킨다() {
        metrics.incrementOptimisticRetry();

        assertEquals(1.0, registry.get("gold.rush.optimistic.lock.retry")
                .tag("strategy", "optimistic").counter().count());
    }

    @Test
    void 고카디널리티_식별자_태그를_등록하지_않는다() {
        metrics.incrementMiningSuccess(LockStrategy.NONE);
        metrics.incrementMiningFailure(LockStrategy.NONE, MiningFailureType.UNKNOWN);
        Timer.Sample sample = metrics.startLockWait();
        metrics.stopLockWait(sample, LockStrategy.NONE);

        Set<String> tagKeys = registry.getMeters().stream()
                .map(Meter::getId)
                .flatMap(id -> id.getTags().stream())
                .map(tag -> tag.getKey().toLowerCase())
                .collect(Collectors.toSet());

        assertTrue(tagKeys.contains("strategy"));
        assertTrue(tagKeys.contains("exception"));
        assertFalse(tagKeys.contains("user_id"));
        assertFalse(tagKeys.contains("mine_id"));
        assertFalse(tagKeys.contains("session_id"));
    }
}
