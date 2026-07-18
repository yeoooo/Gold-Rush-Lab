package io.devyeoooo.Gold_Rush_Lab.mining_log.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MiningLogJpaAdapter implements MiningLogRepository {

    private final MiningLogJpaRepository jpaRepository;
}
