package io.devyeoooo.Gold_Rush_Lab.messaging.mine;

import java.time.Instant;
import java.util.UUID;

public record MiningRequestedEvent(
        UUID eventId,
        UUID userSessionId,
        Long mineId,
        Long amount,
        Instant requestedAt
) {
}
