package io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity.ProcessedMiningEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedMiningEventJpaRepository
        extends JpaRepository<ProcessedMiningEventEntity, UUID> {
}
