package io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine;

import io.devyeoooo.Gold_Rush_Lab.messaging.mine.MiningCompletedEvent;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningMessagingMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class MineSseEmitterManager {

    private static final long TIMEOUT_MILLIS = 30 * 60_000L;

    private final Map<UUID, SseEmitter> emitters =
            new ConcurrentHashMap<>();
    private final MiningMessagingMetrics miningMetrics;

    public SseEmitter connect(UUID userSessionId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);

        SseEmitter previousEmitter = emitters.put(userSessionId, emitter);
        if (previousEmitter != null) {
            previousEmitter.complete();
        }

        emitter.onCompletion(() -> remove(userSessionId, emitter));
        emitter.onTimeout(() -> remove(userSessionId, emitter));
        emitter.onError(error -> remove(userSessionId, emitter));

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data(userSessionId.toString())
            );
        } catch (IOException | IllegalStateException exception) {
            remove(userSessionId, emitter);
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    @Scheduled(
            fixedDelayString =
                    "${gold-rush.sse.heartbeat-interval-millis:15000}"
    )
    public void sendHeartbeat() {
        emitters.forEach((userSessionId, emitter) -> {
            try {
                emitter.send(
                        SseEmitter.event().comment("heartbeat")
                );
            } catch (IOException | IllegalStateException exception) {
                remove(userSessionId, emitter);

                log.debug(
                        "SSE heartbeat 전송 실패. userSessionId={}",
                        userSessionId,
                        exception
                );
            }
        });
    }

    public void completeAll() {
        emitters.forEach((userSessionId, emitter) -> {
            if (emitters.remove(userSessionId, emitter)) {
                try {
                    emitter.complete();
                } catch (IllegalStateException exception) {
                    log.debug(
                            "SSE 연결 종료 실패. userSessionId={}",
                            userSessionId,
                            exception
                    );
                }
            }
        });
    }

    public void send(
            UUID userSessionId,
            MiningCompletedEvent event
    ) {
        SseEmitter emitter = emitters.get(userSessionId);

        if (emitter == null) {
            miningMetrics.incrementSseDelivery("no_subscriber");
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .id(event.eventId().toString())
                            .name("mining-completed")
                            .data(event)
            );
            miningMetrics.incrementSseDelivery("success");
        } catch (IOException | IllegalStateException e) {
            miningMetrics.incrementSseDelivery("failure");
            remove(userSessionId, emitter);

            log.debug(
                    "SSE 전송 실패. userSessionId={}",
                    userSessionId,
                    e
            );
        }
    }

    private void remove(
            UUID userSessionId,
            SseEmitter emitter
    ) {
        emitters.remove(userSessionId, emitter);
    }
}
