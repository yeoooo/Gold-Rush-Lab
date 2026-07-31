package io.devyeoooo.Gold_Rush_Lab.mine.repository;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;

public interface MineRepository {
    Long save(MineEntity entity);
    int decreaseRemainingAmount(Long mineId, Long amount);
    MineEntity findFirstNotDepleted();
    MineEntity findById(Long id);
}
