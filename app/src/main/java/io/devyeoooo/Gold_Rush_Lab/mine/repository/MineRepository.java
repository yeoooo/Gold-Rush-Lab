package io.devyeoooo.Gold_Rush_Lab.mine.repository;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;

public interface MineRepository {
    MineEntity findFirstNotDepleted();
}
