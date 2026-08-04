package io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity.ProcessedMiningEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProcessedMiningEventJpaAdapter implements ProcessedMiningEventRepository {

    private final ProcessedMiningEventJpaRepository jpaRepository;

    @Override
    public Optional<ProcessedMiningEventEntity> findById(UUID eventId) {
        return jpaRepository.findById(eventId);
    }

    @Override
    public void save(ProcessedMiningEventEntity event) {
        jpaRepository.save(event);
    }
}
