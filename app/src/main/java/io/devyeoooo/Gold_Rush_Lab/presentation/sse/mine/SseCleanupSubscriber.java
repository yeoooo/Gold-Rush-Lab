package io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseCleanupSubscriber implements MessageListener {

    private final MineSseEmitterManager emitterManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        emitterManager.completeAll();
    }
}
