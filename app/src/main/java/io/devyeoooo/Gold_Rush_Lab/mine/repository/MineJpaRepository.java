package io.devyeoooo.Gold_Rush_Lab.mine.repository;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MineJpaRepository extends JpaRepository<MineEntity, Long> {
    Optional<MineEntity> findFirstByRemainingAmountGreaterThanOrderByIdAsc(long remainingAmount);

    Optional<MineEntity> findById(Long mineId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select m
        from MineEntity m
        where m.id = :id
    """)
    Optional<MineEntity> findByIdForUpdate(@Param("id") Long id);
}
