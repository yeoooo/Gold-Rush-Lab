package io.devyeoooo.Gold_Rush_Lab_Consumer.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class MiningMetrics {

    private static final String FAILURE_REASON_TAG = "reason";

    private final MeterRegistry meterRegistry;

    public MiningMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopProcessing(Timer.Sample sample) {
        sample.stop(Timer.builder("gold.rush.consumer.mining.processing")
                .description("Mining database processing latency in the consumer")
                .publishPercentileHistogram()
                .register(meterRegistry));
    }

    public void stopPublish(Timer.Sample sample) {
        sample.stop(Timer.builder("gold.rush.consumer.mining.publish")
                .description("Mining completion event publish latency")
                .publishPercentileHistogram()
                .register(meterRegistry));
    }

    public void incrementSuccess() {
        Counter.builder("gold.rush.consumer.mining.success")
                .description("Number of mining requests processed and published successfully")
                .register(meterRegistry)
                .increment();
    }

    public void incrementFailure(MiningFailureType failureType) {
        Counter.builder("gold.rush.consumer.mining.failure")
                .description("Number of failed mining requests")
                .tag(FAILURE_REASON_TAG, failureType.tagValue())
                .register(meterRegistry)
                .increment();
    }

    public void incrementDuplicate() {
        Counter.builder("gold.rush.consumer.mining.duplicate")
                .description("Number of duplicate mining events handled idempotently")
                .register(meterRegistry)
                .increment();
    }

    public void incrementReceived(int partition) {
        Counter.builder("gold.rush.consumer.mining.received")
                .description("Mining request records received by partition")
                .tag("partition", Integer.toString(partition))
                .register(meterRegistry)
                .increment();
    }

    public void incrementDbCommit() {
        Counter.builder("gold.rush.consumer.mining.db.commit")
                .description("Committed non-duplicate mining transactions")
                .register(meterRegistry)
                .increment();
    }

    public void incrementOrderViolation(int partition) {
        Counter.builder("gold.rush.consumer.mining.order.violation")
                .description("Non-increasing Kafka offsets observed by partition")
                .tag("partition", Integer.toString(partition))
                .register(meterRegistry)
                .increment();
    }
}
