package io.devyeoooo.Gold_Rush_Lab.user.service;

import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;

import java.util.UUID;

public interface UserService {
    Long create();
    UserEntity findById(Long id);
    UserEntity findBySessionId(UUID sessionId);
}
