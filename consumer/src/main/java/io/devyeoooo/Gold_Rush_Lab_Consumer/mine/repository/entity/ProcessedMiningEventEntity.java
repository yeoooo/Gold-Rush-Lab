package io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity;

import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningCompletedEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "processed_mining_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedMiningEventEntity {

    @Id
    private UUID eventId;

    private UUID userSessionId;
    private Long mineId;
    private Long minedAmount;
    private Long remainingAmount;
    private Instant requestedAt;

    private ProcessedMiningEventEntity(MiningCompletedEvent event) {
        this.eventId = event.eventId();
        this.userSessionId = event.userSessionId();
        this.mineId = event.mineId();
        this.minedAmount = event.minedAmount();
        this.remainingAmount = event.remainingAmount();
        this.requestedAt = event.requestedAt();
    }

    public static ProcessedMiningEventEntity from(MiningCompletedEvent event) {
        return new ProcessedMiningEventEntity(event);
    }

    public MiningCompletedEvent toEvent() {
        return new MiningCompletedEvent(
                eventId, userSessionId, mineId, minedAmount, remainingAmount, requestedAt
        );
    }
}
