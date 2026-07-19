package io.devyeoooo.Gold_Rush_Lab.mine.service.impl;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.MineRepository;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import io.devyeoooo.Gold_Rush_Lab.mine.service.MineService;
import io.devyeoooo.Gold_Rush_Lab.mining_log.repository.MiningLogRepository;
import io.devyeoooo.Gold_Rush_Lab.mining_log.repository.entity.MiningLogEntity;
import io.devyeoooo.Gold_Rush_Lab.observability.LockStrategy;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningFailureClassifier;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningFailureType;
import io.devyeoooo.Gold_Rush_Lab.observability.MiningMetrics;
import io.devyeoooo.Gold_Rush_Lab.user.repository.UserRepository;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MineServiceImpl implements MineService {

    private final MineRepository mineRepository;
    private final UserRepository userRepository;
    private final MiningLogRepository miningLogRepository;
    private final MiningMetrics miningMetrics;
    private final MiningFailureClassifier miningFailureClassifier;

    @Value("${gold-rush.mining.lock-strategy:none}")
    private String configuredLockStrategy;

    /**
     * Mine 생성 함수
     * 광산의 총량을 입력하여 Mine 생성
     *
     * @param amount
     * @return 생성된 Mine의 Id
     */
    @Override
    @Transactional
    public Long create(Long amount) {
        MineEntity created = MineEntity.create(amount);
        return mineRepository.save(created);
    }

    /**
     * 활성 Mine 조회 함수
     *
     * @return Id 오름차순, remainingAmount > 0 인 첫번째 Mine
     */
    @Override
    @Transactional
    public MineEntity findFirstNotDepleted() {
        return mineRepository.findFirstNotDepleted();
    }

    /**
     * id 기반 Mine 조회
     *
     * 난독화는 되어있지 않음.
     *
     * @param id
     * @return
     */
    @Override
    public MineEntity findById(Long id) {
        return mineRepository.findById(id);
    }

    /**
     * 채굴 함수
     *
     * 1. sessionId 기반 유저 조회, 유저의 금광 조회
     * 2. 유저의 골드 증가
     * 3. 광산의 잔량 감소
     * 4. 로그 생성
     *
     * @param sessionId
     * @param amount
     */
    @Override
    @Transactional
    public void mine(UUID sessionId, Long amount) {
        LockStrategy strategy = LockStrategy.from(configuredLockStrategy);
        try {
            UserEntity foundUser = userRepository.findBySessionId(sessionId);
            MineEntity foundMine = foundUser.getMine();

            foundMine.mine(amount);
            foundUser.addGold(amount);

            miningLogRepository.save(
                    MiningLogEntity.create(foundUser, foundMine, amount)
            );
            recordSuccessAfterCommit(strategy);
        } catch (RuntimeException exception) {
            MiningFailureType failureType = miningFailureClassifier.classify(exception);
            miningMetrics.recordFailure(strategy, failureType);
            throw exception;
        }
    }

    private void recordSuccessAfterCommit(LockStrategy strategy) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 관리되는 트랜잭션 밖에서 직접 호출된 경우 메서드가 반환되면 처리가 완료된 것으로 본다.
            miningMetrics.incrementMiningSuccess(strategy);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                miningMetrics.incrementMiningSuccess(strategy);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    // 커밋 단계의 실패는 애플리케이션 예외 유형을 안정적으로 확인할 수 없다.
                    miningMetrics.recordFailure(strategy, MiningFailureType.UNKNOWN);
                }
            }
        });
    }
}
