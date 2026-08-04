package io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine;

import java.time.Instant;
import java.util.UUID;

public record MiningRequestedEvent(
        UUID eventId,
        UUID userSessionId,
        Long mineId,
        Long amount,
        Instant requestedAt
) {
    public MiningRequestedEvent {
        if (eventId == null || userSessionId == null || mineId == null || requestedAt == null) {
            throw new IllegalArgumentException("이벤트, 사용자 세션, 광산 식별자는 필수입니다.");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("채굴량은 양수여야 합니다.");
        }
    }
}
