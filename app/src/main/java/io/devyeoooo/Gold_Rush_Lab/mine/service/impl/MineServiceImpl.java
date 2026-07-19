package io.devyeoooo.Gold_Rush_Lab.mine.service.impl;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.MineRepository;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import io.devyeoooo.Gold_Rush_Lab.mine.service.MineService;
import io.devyeoooo.Gold_Rush_Lab.mining_log.repository.MiningLogRepository;
import io.devyeoooo.Gold_Rush_Lab.mining_log.repository.entity.MiningLogEntity;
import io.devyeoooo.Gold_Rush_Lab.user.repository.UserRepository;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MineServiceImpl implements MineService {

    private final MineRepository mineRepository;
    private final UserRepository userRepository;
    private final MiningLogRepository miningLogRepository;

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
        UserEntity foundUser = userRepository.findBySessionId(sessionId);
        MineEntity foundMine = foundUser.getMine();

        foundMine.mine(amount);
        foundUser.addGold(amount);

        miningLogRepository.save(
                MiningLogEntity.create(foundUser, foundMine, amount)
        );
    }
}
