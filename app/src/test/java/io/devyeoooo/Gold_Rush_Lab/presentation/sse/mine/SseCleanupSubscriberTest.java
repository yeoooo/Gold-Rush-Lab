package io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SseCleanupSubscriberTest {

    @Test
    void cleanup_신호를_받으면_모든_emitter를_정리한다() {
        MineSseEmitterManager emitterManager =
                mock(MineSseEmitterManager.class);
        SseCleanupSubscriber subscriber =
                new SseCleanupSubscriber(emitterManager);

        subscriber.onMessage(mock(Message.class), null);

        verify(emitterManager).completeAll();
    }
}
