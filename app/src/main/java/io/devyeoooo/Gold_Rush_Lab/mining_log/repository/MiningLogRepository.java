package io.devyeoooo.Gold_Rush_Lab.mining_log.repository;

import io.devyeoooo.Gold_Rush_Lab.mining_log.repository.entity.MiningLogEntity;

public interface MiningLogRepository {
    Long save(MiningLogEntity miningLog);
}
