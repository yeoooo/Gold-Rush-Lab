package io.devyeoooo.Gold_Rush_Lab.mine.service;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.LockAcquireException;
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

    public void mine(UUID userSessionId, Long mineId) {

        RLock lock = redissonClient.getLock(
                "gold-rush:mine:" + mineId);

        boolean acquired = false;

        try {

            acquired = lock.tryLock(3, TimeUnit.SECONDS);

            if (!acquired) {
                throw new LockAcquireException();
            }

            mineService.mine(userSessionId, mineId);

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
