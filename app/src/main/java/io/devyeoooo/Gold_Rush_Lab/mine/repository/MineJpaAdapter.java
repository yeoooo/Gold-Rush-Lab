package io.devyeoooo.Gold_Rush_Lab.mine.repository;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.ActiveMineNotFoundException;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MineJpaAdapter implements MineRepository {

    private final MineJpaRepository mineJpaRepository;

    @Override
    public Long save(MineEntity entity) {
        return mineJpaRepository.save(entity).getId();
    }

    @Override
    public MineEntity findFirstNotDepleted() {
        return mineJpaRepository.findFirstByRemainingAmountGreaterThanOrderByIdAsc(0)
                .orElseThrow(ActiveMineNotFoundException::new);
    }
}
