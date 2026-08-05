package io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity.MineEntity;
import java.util.Optional;

public interface MineRepository {
    Optional<MineEntity> findById(Long id);

    int decreaseRemainingAmount(Long mineId, Long amount);
}
