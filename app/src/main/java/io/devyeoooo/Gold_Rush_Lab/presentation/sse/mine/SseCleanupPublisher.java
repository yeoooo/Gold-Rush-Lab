package io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static io.devyeoooo.Gold_Rush_Lab.config.RedisPubSubConfig.SSE_CLEANUP_CHANNEL;

@Component
@RequiredArgsConstructor
public class SseCleanupPublisher {

    private static final String CLEANUP_MESSAGE = "cleanup";

    private final StringRedisTemplate redisTemplate;

    public void publish() {
        redisTemplate.convertAndSend(
                SSE_CLEANUP_CHANNEL,
                CLEANUP_MESSAGE
        );
    }
}
