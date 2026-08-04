package io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine;

import io.devyeoooo.Gold_Rush_Lab.messaging.mine.MiningCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class MiningCompletedPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChannelTopic miningCompletedTopic;

    /**
     * 채굴 완료 이벤트를 Redis Pub/Sub 채널에 발행하는 함수.
     *
     * 1. 채굴 완료 이벤트를 JSON payload로 직렬화한다.
     * 2. payload를 채굴 완료 Redis channel에 발행한다.
     * 3. 직렬화에 실패하면 IllegalStateException으로 변환해 던진다.
     *
     * @param event 발행할 채굴 완료 이벤트
     * @throws IllegalStateException 이벤트를 JSON으로 직렬화할 수 없는 경우
     */
    public void publish(MiningCompletedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            redisTemplate.convertAndSend(
                    miningCompletedTopic.getTopic(),
                    payload
            );
        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "채굴 완료 이벤트 직렬화에 실패했습니다.",
                    e
            );
        }
    }
}
