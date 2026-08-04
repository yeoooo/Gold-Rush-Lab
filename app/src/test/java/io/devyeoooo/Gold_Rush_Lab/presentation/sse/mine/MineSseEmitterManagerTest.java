package io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine;

import io.devyeoooo.Gold_Rush_Lab.observability.MiningMessagingMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MineSseEmitterManagerTest {

    private MineSseEmitterManager emitterManager;
    private Map<UUID, SseEmitter> emitters;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws ReflectiveOperationException {
        emitterManager = new MineSseEmitterManager(
                mock(MiningMessagingMetrics.class)
        );

        Field emittersField = MineSseEmitterManager.class
                .getDeclaredField("emitters");
        emittersField.setAccessible(true);
        emitters = (Map<UUID, SseEmitter>) emittersField.get(emitterManager);
    }

    @Test
    void 활성화된_emitter에_heartbeat를_전송한다() throws IOException {
        UUID sessionId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        emitters.put(sessionId, emitter);

        emitterManager.sendHeartbeat();

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        assertTrue(emitters.containsKey(sessionId));
    }

    @Test
    void heartbeat_전송에_실패한_emitter를_제거한다() throws IOException {
        UUID sessionId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        emitters.put(sessionId, emitter);
        doThrow(new IOException("disconnected"))
                .when(emitter)
                .send(any(SseEmitter.SseEventBuilder.class));

        emitterManager.sendHeartbeat();

        assertFalse(emitters.containsKey(sessionId));
    }

    @Test
    void 모든_emitter를_제거하고_연결을_완료한다() {
        SseEmitter firstEmitter = mock(SseEmitter.class);
        SseEmitter secondEmitter = mock(SseEmitter.class);
        emitters.put(UUID.randomUUID(), firstEmitter);
        emitters.put(UUID.randomUUID(), secondEmitter);

        emitterManager.completeAll();

        assertTrue(emitters.isEmpty());
        verify(firstEmitter).complete();
        verify(secondEmitter).complete();
    }
}
