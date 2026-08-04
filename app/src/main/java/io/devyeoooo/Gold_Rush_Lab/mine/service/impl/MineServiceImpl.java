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
     * 지정한 총량을 가진 광산을 생성하는 함수.
     *
     * 1. 입력받은 총량으로 광산 엔티티를 생성한다.
     * 2. 광산을 저장하고 생성된 ID를 반환한다.
     *
     * @param amount 광산의 초기 보유량
     * @return 생성된 Mine의 Id
     */
    @Override
    @Transactional
    public Long create(Long amount) {
        MineEntity created = MineEntity.create(amount);
        return mineRepository.save(created);
    }

    /**
     * 채굴 가능한 광산 중 ID가 가장 작은 광산을 조회하는 함수.
     *
     * 1. 잔량이 남은 광산을 ID 오름차순으로 조회한다.
     * 2. 가장 먼저 발견된 광산을 반환한다.
     *
     * @return ID 오름차순으로 처음 발견된 잔량이 있는 광산
     */
    @Override
    @Transactional
    public MineEntity findFirstNotDepleted() {
        return mineRepository.findFirstNotDepleted();
    }

    /**
     * ID로 광산을 조회하는 함수.
     *
     * 1. 입력받은 ID를 Repository에 전달한다.
     * 2. 조회된 광산을 반환한다.
     *
     * @param id 조회할 광산 ID
     * @return 조회된 광산
     */
    @Override
    public MineEntity findById(Long id) {
        return mineRepository.findById(id);
    }

    /**
     * 세션 사용자가 속한 광산을 채굴하고 관련 데이터를 한 트랜잭션에서 변경하는 함수.
     *
     * 1. 설정된 잠금 전략과 sessionId에 해당하는 사용자를 조회한다.
     * 2. 사용자가 속한 광산의 잔량을 감소시키고 사용자 골드를 증가시킨다.
     * 3. 채굴 로그를 저장하고 commit 이후 성공 메트릭을 기록하도록 등록한다.
     * 4. 실패하면 예외를 분류해 실패 메트릭을 기록한 뒤 다시 던진다.
     *
     * @param sessionId 채굴을 요청한 사용자 세션 ID
     * @param amount 채굴할 양
     * @throws RuntimeException 사용자 조회 또는 채굴과 데이터 저장에 실패한 경우
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

    /**
     * DB commit 결과에 따라 채굴 성공 또는 실패 메트릭을 기록하는 함수.
     *
     * 1. 트랜잭션 동기화가 비활성 상태이면 즉시 성공 메트릭을 기록한다.
     * 2. 동기화가 활성 상태이면 트랜잭션 완료 콜백을 등록한다.
     * 3. commit이 완료되면 성공 메트릭을 기록한다.
     * 4. commit되지 않으면 UNKNOWN 실패 메트릭을 기록한다.
     *
     * @param strategy 메트릭 label에 기록할 잠금 전략
     */
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
