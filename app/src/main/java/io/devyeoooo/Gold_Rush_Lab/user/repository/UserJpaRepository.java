package io.devyeoooo.Gold_Rush_Lab.user.repository;

import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
}
