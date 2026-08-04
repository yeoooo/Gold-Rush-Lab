package io.devyeoooo.Gold_Rush_Lab_Consumer.observability;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiningMetricsTest {

    private SimpleMeterRegistry registry;
    private MiningMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new MiningMetrics(registry);
    }

    @Test
    void 성공_실패_중복을_기록한다() {
        metrics.incrementSuccess();
        metrics.incrementFailure(MiningFailureType.MINE_DEPLETED);
        metrics.incrementDuplicate();
        metrics.incrementReceived(1);
        metrics.incrementDbCommit();
        metrics.incrementOrderViolation(1);

        assertEquals(1.0, registry.get("gold.rush.consumer.mining.success").counter().count());
        assertEquals(1.0, registry.get("gold.rush.consumer.mining.failure")
                .tag("reason", "mine_depleted").counter().count());
        assertEquals(1.0, registry.get("gold.rush.consumer.mining.duplicate").counter().count());
        assertEquals(1.0, registry.get("gold.rush.consumer.mining.received")
                .tag("partition", "1").counter().count());
        assertEquals(1.0, registry.get("gold.rush.consumer.mining.db.commit").counter().count());
        assertEquals(1.0, registry.get("gold.rush.consumer.mining.order.violation")
                .tag("partition", "1").counter().count());
    }

    @Test
    void DB_처리와_Kafka_발행_시간을_기록한다() {
        Timer.Sample processing = metrics.startTimer();
        Timer.Sample publish = metrics.startTimer();

        metrics.stopProcessing(processing);
        metrics.stopPublish(publish);

        assertEquals(1L, registry.get("gold.rush.consumer.mining.processing").timer().count());
        assertEquals(1L, registry.get("gold.rush.consumer.mining.publish").timer().count());
    }
}
