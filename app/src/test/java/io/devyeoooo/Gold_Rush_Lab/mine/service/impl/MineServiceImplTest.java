package io.devyeoooo.Gold_Rush_Lab.mine.service.impl;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.MineRepository;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import io.devyeoooo.Gold_Rush_Lab.mining_log.repository.MiningLogRepository;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningFailureClassifier;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningMetrics;
import io.devyeoooo.Gold_Rush_Lab.user.repository.UserRepository;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MineServiceImplTest {

    @Mock
    private MineRepository mineRepository;

    @Mock
    private MiningLogRepository miningLogRepository;

    @Mock
    private UserRepository userRepository;

    private MineServiceImpl mineService;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        mineService = new MineServiceImpl(
                mineRepository,
                userRepository,
                miningLogRepository,
                new MiningMetrics(meterRegistry),
                new MiningFailureClassifier()
        );
    }

    @Test
    void 세션_식별자의_사용자가_소속된_광산에서_금을_하나_채굴한다() {
        MineEntity mine = MineEntity.create(100L);
        UserEntity user = UserEntity.create(mine);
        UUID sessionId = user.getSessionId();
        when(userRepository.findBySessionId(sessionId)).thenReturn(user);

        mineService.mine(sessionId, 1L);

        verify(userRepository).findBySessionId(sessionId);
        assertEquals(99L, mine.getRemainingAmount());
        assertEquals(1L, user.getTotalMinedGold());
        assertEquals(1.0, meterRegistry.get("gold.rush.mining.success")
                .tag("strategy", "none").counter().count());
    }

    @Test
    void 채굴_실패를_기록하고_기존_예외를_다시_던진다() {
        MineEntity mine = MineEntity.create(100L);
        UserEntity user = UserEntity.create(mine);
        UUID sessionId = user.getSessionId();
        RuntimeException failure = new RuntimeException("save failed");
        when(userRepository.findBySessionId(sessionId)).thenReturn(user);
        doThrow(failure).when(miningLogRepository).save(org.mockito.ArgumentMatchers.any());

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> mineService.mine(sessionId, 1L)
        );

        assertSame(failure, thrown);
        assertEquals(1.0, meterRegistry.get("gold.rush.mining.failure")
                .tag("strategy", "none")
                .tag("exception", "unknown")
                .counter().count());
    }

    @Test
    void 트랜잭션이_활성화되면_커밋_이후에만_성공을_기록한다() {
        MineEntity mine = MineEntity.create(100L);
        UserEntity user = UserEntity.create(mine);
        UUID sessionId = user.getSessionId();
        when(userRepository.findBySessionId(sessionId)).thenReturn(user);

        TransactionSynchronizationManager.initSynchronization();
        try {
            mineService.mine(sessionId, 1L);

            assertTrue(meterRegistry.find("gold.rush.mining.success").counter() == null);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(
                            TransactionSynchronization.STATUS_COMMITTED
                    ));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertEquals(1.0, meterRegistry.get("gold.rush.mining.success")
                .tag("strategy", "none").counter().count());
    }

    @Test
    void 식별자로_광산을_조회한다() {
        MineEntity mine = MineEntity.create(100L);
        when(mineRepository.findById(1L)).thenReturn(mine);

        MineEntity found = mineService.findById(1L);

        assertSame(mine, found);
        verify(mineRepository).findById(1L);
    }
}
