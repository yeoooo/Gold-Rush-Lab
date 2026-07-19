package io.devyeoooo.Gold_Rush_Lab.observability;

import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningPrometheusExposureTest {

    @Test
    void 커스텀_메트릭을_Prometheus_이름으로_노출한다() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        MiningMetrics metrics = new MiningMetrics(registry);

        metrics.incrementMiningSuccess(LockStrategy.PESSIMISTIC);
        metrics.recordFailure(LockStrategy.PESSIMISTIC, MiningFailureType.LOCK_TIMEOUT);
        metrics.recordFailure(LockStrategy.PESSIMISTIC, MiningFailureType.DEADLOCK);
        metrics.incrementOptimisticRetry();
        Timer.Sample sample = metrics.startLockWait();
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        metrics.stopLockWait(sample, LockStrategy.PESSIMISTIC);

        String scrape = registry.scrape();
        assertTrue(scrape.contains("gold_rush_mining_success_total"));
        assertTrue(scrape.contains("gold_rush_mining_failure_total"));
        assertTrue(scrape.contains("gold_rush_lock_timeout_total"));
        assertTrue(scrape.contains("gold_rush_deadlock_total"));
        assertTrue(scrape.contains("gold_rush_optimistic_lock_retry_total"));
        assertTrue(scrape.contains("gold_rush_mining_lock_wait_seconds_count"));
        assertTrue(scrape.contains("gold_rush_mining_lock_wait_seconds_sum"));
        assertTrue(scrape.contains("gold_rush_mining_lock_wait_seconds_max"));
        assertTrue(scrape.contains("gold_rush_mining_lock_wait_seconds_bucket"));
    }
}
