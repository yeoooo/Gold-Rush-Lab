package io.devyeoooo.Gold_Rush_Lab.user.repository;

import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository {
    UserEntity save(UserEntity user);
    UserEntity findById(Long id);
    UserEntity findBySessionId(UUID sessionId);
}
