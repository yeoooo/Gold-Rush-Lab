package io.devyeoooo.Gold_Rush_Lab.observability;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiningMessagingMetricsTest {

    private SimpleMeterRegistry registry;
    private MiningMessagingMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new MiningMessagingMetrics(registry);
    }

    @Test
    void producer와_SSE_카운터를_기록한다() {
        metrics.incrementProducerSuccess();
        metrics.incrementProducerFailure("kafka");
        metrics.incrementSseDelivery("success");

        assertEquals(1.0, registry.get("gold.rush.producer.mining.success").counter().count());
        assertEquals(1.0, registry.get("gold.rush.producer.mining.failure")
                .tag("reason", "kafka").counter().count());
        assertEquals(1.0, registry.get("gold.rush.sse.mining.delivery")
                .tag("result", "success").counter().count());
    }

    @Test
    void producer_발행과_E2E_시간을_기록한다() {
        Timer.Sample publish = metrics.startTimer();
        metrics.stopProducerPublish(publish);
        metrics.recordEndToEnd(Instant.now().minusMillis(10));

        assertEquals(1L, registry.get("gold.rush.producer.mining.publish").timer().count());
        assertEquals(1L, registry.get("gold.rush.mining.end.to.end").timer().count());
    }
}
