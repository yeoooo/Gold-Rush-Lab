package io.devyeoooo.Gold_Rush_Lab.mine.repository;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MineJpaRepository extends JpaRepository<MineEntity, Long> {
    Optional<MineEntity> findFirstByRemainingAmountGreaterThanOrderByIdAsc(long remainingAmount);
}
