package io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine;

import io.devyeoooo.Gold_Rush_Lab.messaging.mine.MiningCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class MiningCompletedSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final MineSseEmitterManager emitterManager;

    /**
     * Redis Pub/Sub의 채굴 완료 메시지를 해당 사용자 SSE 연결로 전달하는 함수.
     *
     * 1. Redis 메시지 body를 UTF-8 문자열로 변환한다.
     * 2. JSON payload를 채굴 완료 이벤트로 역직렬화한다.
     * 3. userSessionId에 해당하는 SSE emitter로 이벤트를 전송한다.
     * 4. 역직렬화에 실패하면 이벤트를 전달하지 않고 오류 로그를 남긴다.
     *
     * @param message Redis에서 수신한 채굴 완료 메시지
     * @param pattern 메시지 수신에 사용된 Redis channel pattern
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload =
                    new String(message.getBody(), StandardCharsets.UTF_8);

            MiningCompletedEvent event =
                    objectMapper.readValue(
                            payload,
                            MiningCompletedEvent.class
                    );

            emitterManager.send(
                    event.userSessionId(),
                    event
            );

        } catch (JacksonException e) {
            log.error(
                    "Redis 채굴 완료 이벤트 역직렬화 실패. payload={}",
                    new String(message.getBody(), StandardCharsets.UTF_8),
                    e
            );
        }
    }
}
