package io.devyeoooo.Gold_Rush_Lab.messaging.mine.kafka;

import io.devyeoooo.Gold_Rush_Lab.messaging.mine.MiningCompletedEvent;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningMessagingMetrics;
import io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine.MiningCompletedPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaMiningCompletedListener {

    private final ObjectMapper objectMapper;
    private final MiningCompletedPublisher publisher;
    private final MiningMessagingMetrics miningMetrics;

    /**
     * Kafka에서 수신한 채굴 완료 메시지를 Redis Pub/Sub으로 중계하는 함수.
     *
     * 1. JSON payload를 채굴 완료 이벤트로 역직렬화한다.
     * 2. 채굴 요청부터 완료 메시지 수신까지의 end-to-end 시간을 기록한다.
     * 3. 채굴 완료 이벤트를 Redis Pub/Sub으로 발행한다.
     * 4. 역직렬화에 실패하면 이벤트를 전달하지 않고 오류 로그를 남긴다.
     *
     * @param payload JSON 형식의 채굴 완료 이벤트
     */
    @KafkaListener(topics = "mining-completed")
    public void consume(String payload) {
        try {
            MiningCompletedEvent event = objectMapper.readValue(payload, MiningCompletedEvent.class);
            miningMetrics.recordEndToEnd(event.requestedAt());
            publisher.publish(event);
        } catch (JacksonException exception) {
            log.error("Kafka 채굴 완료 이벤트 역직렬화 실패. payload={}", payload, exception);
        }
    }
}
