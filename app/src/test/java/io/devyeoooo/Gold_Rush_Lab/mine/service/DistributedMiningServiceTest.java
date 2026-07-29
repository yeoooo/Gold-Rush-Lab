package io.devyeoooo.Gold_Rush_Lab.mine.service;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.LockAcquireException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedMiningServiceTest {

    private static final Long MINE_ID = 1L;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private MineService mineService;

    private DistributedMiningService distributedMiningService;

    @BeforeEach
    void setUp() {
        distributedMiningService = new DistributedMiningService(redissonClient, mineService);
        when(redissonClient.getLock("gold-rush:mine:" + MINE_ID)).thenReturn(lock);
    }

    @AfterEach
    void clearInterruptStatus() {
        Thread.interrupted();
    }

    @Test
    void 광산_식별자를_기준으로_락을_획득한_뒤_채굴한다() throws InterruptedException {
        UUID sessionId = UUID.randomUUID();
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        distributedMiningService.mine(sessionId, MINE_ID);

        verify(redissonClient).getLock("gold-rush:mine:" + MINE_ID);
        verify(mineService).mine(sessionId, MINE_ID);
        verify(lock).unlock();
    }

    @Test
    void 락을_획득하지_못하면_채굴하지_않는다() throws InterruptedException {
        UUID sessionId = UUID.randomUUID();
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(false);

        LockAcquireException exception = assertThrows(
                LockAcquireException.class,
                () -> distributedMiningService.mine(sessionId, MINE_ID)
        );

        assertEquals("분산 락을 얻는데 실패했습니다.", exception.getMessage());
        verify(mineService, never()).mine(sessionId, MINE_ID);
        verify(lock, never()).unlock();
    }

    @Test
    void 락_대기_중_인터럽트되면_인터럽트_상태를_복원한다() throws InterruptedException {
        UUID sessionId = UUID.randomUUID();
        InterruptedException interruptedException = new InterruptedException("interrupted");
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenThrow(interruptedException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> distributedMiningService.mine(sessionId, MINE_ID)
        );

        assertEquals(interruptedException, exception.getCause());
        assertTrue(Thread.currentThread().isInterrupted());
        verify(mineService, never()).mine(sessionId, MINE_ID);
        verify(lock, never()).unlock();
    }

    @Test
    void 채굴에서_예외가_발생해도_보유한_락을_해제한다() throws InterruptedException {
        UUID sessionId = UUID.randomUUID();
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new IllegalStateException("mining failed"))
                .when(mineService).mine(sessionId, MINE_ID);

        assertThrows(
                IllegalStateException.class,
                () -> distributedMiningService.mine(sessionId, MINE_ID)
        );

        verify(lock).unlock();
    }

    @Test
    void 현재_스레드가_락을_보유하지_않으면_해제하지_않는다() throws InterruptedException {
        UUID sessionId = UUID.randomUUID();
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        distributedMiningService.mine(sessionId, MINE_ID);

        verify(mineService).mine(sessionId, MINE_ID);
        verify(lock, never()).unlock();
    }
}
