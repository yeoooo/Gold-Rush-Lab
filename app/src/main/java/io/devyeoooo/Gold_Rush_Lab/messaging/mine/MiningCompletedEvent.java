package io.devyeoooo.Gold_Rush_Lab.messaging.mine;

import java.time.Instant;
import java.util.UUID;

public record MiningCompletedEvent(
        UUID eventId,
        UUID userSessionId,
        Long mineId,
        Long minedAmount,
        Long remainingAmount,
        Instant requestedAt
) {
}
