package io.devyeoooo.Gold_Rush_Lab.mine.service;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.LockAcquireException;
import io.devyeoooo.Gold_Rush_Lab.observability.LockStrategy;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DistributedMiningService {

    private final RedissonClient redissonClient;
    private final MineService mineService;
    private final MiningMetrics miningMetrics;

    public void mine(UUID userSessionId, Long mineId, Long amount) {

        RLock lock = redissonClient.getLock(
                "gold-rush:mine:" + mineId);

        boolean acquired = false;
        Timer.Sample lockWaitSample = miningMetrics.startLockWait();

        try {

            try {
                acquired = lock.tryLock(60, TimeUnit.SECONDS);
            } finally {
                miningMetrics.stopLockWait(lockWaitSample, LockStrategy.REDIS);
            }

            if (!acquired) {
                miningMetrics.incrementLockTimeout(LockStrategy.REDIS);
                throw new LockAcquireException();
            }

            mineService.mine(userSessionId, amount);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            throw new RuntimeException(e);

        } finally {

            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
