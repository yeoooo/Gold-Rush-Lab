package io.devyeoooo.Gold_Rush_Lab_Consumer.mining_log.repository;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mining_log.repository.entity.MiningLogEntity;

public interface MiningLogRepository {
    void save(MiningLogEntity miningLog);
}
