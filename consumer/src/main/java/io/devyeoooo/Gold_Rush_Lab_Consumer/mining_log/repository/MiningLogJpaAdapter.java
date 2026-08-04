package io.devyeoooo.Gold_Rush_Lab_Consumer.mining_log.repository;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mining_log.repository.entity.MiningLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MiningLogJpaAdapter implements MiningLogRepository {

    private final MiningLogJpaRepository jpaRepository;

    @Override
    public void save(MiningLogEntity miningLog) {
        jpaRepository.save(miningLog);
    }
}
