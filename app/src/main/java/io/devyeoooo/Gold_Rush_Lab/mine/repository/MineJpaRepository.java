package io.devyeoooo.Gold_Rush_Lab.mine.repository;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MineJpaRepository extends JpaRepository<MineEntity, Long> {
    Optional<MineEntity> findFirstByRemainingAmountGreaterThanOrderByIdAsc(long remainingAmount);
    @Modifying(
            flushAutomatically = true
            , clearAutomatically = true
    )
    @Query("""
    update MineEntity m
       set m.remainingAmount = m.remainingAmount - :amount
     where m.id = :mineId
       and m.remainingAmount >= :amount
    """)
    int decreaseRemainingAmount(
            @Param("mineId") Long mineId
            , @Param("amount") Long amount
    );
}
