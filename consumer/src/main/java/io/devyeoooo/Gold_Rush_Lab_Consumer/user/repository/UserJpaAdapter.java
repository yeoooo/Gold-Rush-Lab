package io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository;

import io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserJpaAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public Optional<UserEntity> findBySessionId(UUID sessionId) {
        return jpaRepository.findBySessionId(sessionId);
    }
}
