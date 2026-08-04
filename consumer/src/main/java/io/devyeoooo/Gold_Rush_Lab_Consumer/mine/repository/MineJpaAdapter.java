package io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity.MineEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MineJpaAdapter implements MineRepository {

    private final MineJpaRepository jpaRepository;

    @Override
    public Optional<MineEntity> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
