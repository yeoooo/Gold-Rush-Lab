package io.devyeoooo.Gold_Rush_Lab.mining_log.repository;

import io.devyeoooo.Gold_Rush_Lab.mining_log.repository.entity.MiningLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiningLogJpaRepository extends JpaRepository<MiningLogEntity, Long> {
}
