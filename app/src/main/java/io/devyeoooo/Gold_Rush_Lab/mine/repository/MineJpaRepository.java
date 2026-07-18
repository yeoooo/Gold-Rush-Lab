package io.devyeoooo.Gold_Rush_Lab.mine.repository;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MineJpaRepository extends JpaRepository<MineEntity, Long> {
}
