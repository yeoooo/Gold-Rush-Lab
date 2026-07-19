package io.devyeoooo.Gold_Rush_Lab.mining_log.repository;

import io.devyeoooo.Gold_Rush_Lab.mining_log.repository.entity.MiningLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MiningLogJpaAdapter implements MiningLogRepository {

    private final MiningLogJpaRepository jpaRepository;

    @Override
    public Long save(MiningLogEntity miningLog) {
        return jpaRepository.save(miningLog).getId();
    }
}
