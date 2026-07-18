package io.devyeoooo.Gold_Rush_Lab.mine.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MineJpaAdapter implements MineRepository {

    private final MineJpaRepository mineJpaRepository;

}
