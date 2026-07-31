package io.devyeoooo.Gold_Rush_Lab.user.repository;

import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findBySessionId(UUID sessionId);

    @Modifying(
            flushAutomatically = true
            , clearAutomatically = true
    )
    @Query("""
    update UserEntity u
        set u.totalMinedGold = u.totalMinedGold + :amount
        where u.sessionId=:sessionId
    """)
    int increaseTotalMinedGold(
            @Param("sessionId") UUID sessionId
            , @Param("amount") Long amount
    );
}
