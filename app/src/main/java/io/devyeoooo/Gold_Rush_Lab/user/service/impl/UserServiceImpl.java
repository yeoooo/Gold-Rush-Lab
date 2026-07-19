package io.devyeoooo.Gold_Rush_Lab.user.service.impl;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.MineRepository;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import io.devyeoooo.Gold_Rush_Lab.user.repository.UserRepository;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import io.devyeoooo.Gold_Rush_Lab.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MineRepository mineRepository;

    /**
     * 유저 생성 함수
     * 1. 고갈되지 않은 Mine 조회
     * 2. Mine을 통해 User 생성
     * @return 생성된 유저의 ID
     */
    @Override
    @Transactional
    public Long create() {
        MineEntity foundMine = mineRepository.findFirstNotDepleted();
        return userRepository.save(UserEntity.create(foundMine)).getId();
    }

    /**
     * 유저 Id 기반 단일조회 함수
     * @param id
     * @return 해당 ID 를 가진 유저 엔티티
     */
    @Override
    public UserEntity findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 세션 Id 기반 단일조회 함수
     * @param sessionId
     * @return 해당 session_ID 를 가진 유저 엔티티
     */
    @Override
    public UserEntity findBySessionId(UUID sessionId) {
        return userRepository.findBySessionId(sessionId);
    }
}
