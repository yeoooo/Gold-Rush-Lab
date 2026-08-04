package io.devyeoooo.Gold_Rush_Lab.messaging.mine.kafka;

import io.devyeoooo.Gold_Rush_Lab.messaging.mine.MiningRequestedEvent;
import io.devyeoooo.Gold_Rush_Lab.messaging.mine.MiningRequestedPublisher;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningMessagingMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class KafkaMiningRequestedPublisher implements MiningRequestedPublisher {

    public static final String TOPIC = "mining-requested";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MiningMessagingMetrics miningMetrics;

    /**
     * 채굴 요청 이벤트를 Kafka에 발행하고 발행 결과 메트릭을 기록하는 함수.
     *
     * 1. 채굴 요청 이벤트를 JSON payload로 직렬화한다.
     * 2. 사용자 sessionId를 key로 사용해 mining-requested topic에 메시지를 전송한다.
     * 3. 전송 결과가 확정되면 성공 메트릭을 기록한다.
     * 4. 직렬화 또는 Kafka 전송에 실패하면 실패 메트릭을 기록한 뒤 예외를 던진다.
     * 5. 성공 여부와 관계없이 발행 소요 시간을 기록한다.
     *
     * @param event 발행할 채굴 요청 이벤트
     * @throws IllegalStateException 이벤트를 JSON으로 직렬화할 수 없는 경우
     * @throws RuntimeException Kafka 전송에 실패한 경우
     */
    @Override
    public void publish(MiningRequestedEvent event) {
        Timer.Sample sample = miningMetrics.startTimer();
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.userSessionId().toString(), payload).join();
            miningMetrics.incrementProducerSuccess();
        } catch (JacksonException exception) {
            miningMetrics.incrementProducerFailure("serialization");
            throw new IllegalStateException("채굴 요청 이벤트 직렬화에 실패했습니다.", exception);
        } catch (RuntimeException exception) {
            miningMetrics.incrementProducerFailure("kafka");
            throw exception;
        } finally {
            miningMetrics.stopProducerPublish(sample);
        }
    }
}
