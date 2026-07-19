package io.devyeoooo.Gold_Rush_Lab.user.repository;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.UserNotFoundException;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor

public class UserJpaAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public UserEntity save(UserEntity user) {
        return userJpaRepository.save(user);
    }

    @Override
    public UserEntity findById(Long id) {
        return userJpaRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public UserEntity findBySessionId(UUID sessionId) {
        return userJpaRepository.findBySessionId(sessionId)
                .orElseThrow(UserNotFoundException::new);
    }
}
