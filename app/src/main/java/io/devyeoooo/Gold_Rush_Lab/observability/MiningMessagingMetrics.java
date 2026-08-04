package io.devyeoooo.Gold_Rush_Lab.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class MiningMessagingMetrics {

    private final MeterRegistry meterRegistry;

    public MiningMessagingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopProducerPublish(Timer.Sample sample) {
        sample.stop(timer("gold.rush.producer.mining.publish", "Mining request Kafka publish latency"));
    }

    public void incrementProducerSuccess() {
        counter("gold.rush.producer.mining.success", "Published mining requests").increment();
    }

    public void incrementProducerFailure(String reason) {
        Counter.builder("gold.rush.producer.mining.failure")
                .description("Failed mining request publications")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void recordEndToEnd(Instant requestedAt) {
        if (requestedAt == null) {
            return;
        }
        Duration duration = Duration.between(requestedAt, Instant.now());
        if (!duration.isNegative()) {
            timer("gold.rush.mining.end.to.end", "API request to completed Kafka event latency")
                    .record(duration);
        }
    }

    public void incrementSseDelivery(String result) {
        Counter.builder("gold.rush.sse.mining.delivery")
                .description("Mining result SSE delivery attempts")
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    private Timer timer(String name, String description) {
        return Timer.builder(name)
                .description(description)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    private Counter counter(String name, String description) {
        return Counter.builder(name)
                .description(description)
                .register(meterRegistry);
    }
}
