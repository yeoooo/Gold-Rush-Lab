package io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity.ProcessedMiningEventEntity;
import java.util.Optional;
import java.util.UUID;

public interface ProcessedMiningEventRepository {
    Optional<ProcessedMiningEventEntity> findById(UUID eventId);
    void save(ProcessedMiningEventEntity event);
}
