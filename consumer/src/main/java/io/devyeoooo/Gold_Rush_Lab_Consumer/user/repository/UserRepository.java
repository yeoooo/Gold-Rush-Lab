package io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository;

import io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<UserEntity> findBySessionId(UUID sessionId);
}
