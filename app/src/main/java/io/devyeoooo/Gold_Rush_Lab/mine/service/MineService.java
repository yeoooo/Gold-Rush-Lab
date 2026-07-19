package io.devyeoooo.Gold_Rush_Lab.mine.service;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;

import java.util.UUID;

public interface MineService {
    Long create(Long remainingAmount);

    MineEntity findFirstNotDepleted();

    void mine(UUID sessionId, Long amount);
}
