package io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.kafka;

import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningCompletedEvent;
import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningCompletedPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class KafkaMiningCompletedPublisher implements MiningCompletedPublisher {

    public static final String TOPIC = "mining-completed";

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 채굴 완료 이벤트를 Kafka에 발행하는 함수.
     *
     * 1. 채굴 완료 이벤트를 JSON payload로 직렬화한다.
     * 2. 같은 광산의 이벤트가 같은 partition에 기록되도록 mineId를 key로 사용한다.
     * 3. mining-completed topic에 메시지를 전송하고 결과가 확정될 때까지 대기한다.
     * 4. 직렬화 또는 전송에 실패하면 예외를 호출자에게 전달한다.
     *
     * @param event 발행할 채굴 완료 이벤트
     */
    @Override
    public void publish(MiningCompletedEvent event) {
        kafkaTemplate.send(
                TOPIC,
                event.mineId().toString(),
                serialize(event)
        ).join();
    }

    /**
     * 채굴 완료 이벤트를 Kafka payload로 사용할 JSON 문자열로 변환하는 함수.
     *
     * 1. ObjectMapper를 사용해 이벤트를 JSON 문자열로 직렬화한다.
     * 2. 직렬화에 실패하면 IllegalStateException으로 변환해 던진다.
     *
     * @param event 직렬화할 채굴 완료 이벤트
     * @return JSON 형식의 Kafka payload
     * @throws IllegalStateException 이벤트를 JSON으로 직렬화할 수 없는 경우
     */
    private String serialize(MiningCompletedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("채굴 완료 이벤트 직렬화에 실패했습니다.", exception);
        }
    }
}
