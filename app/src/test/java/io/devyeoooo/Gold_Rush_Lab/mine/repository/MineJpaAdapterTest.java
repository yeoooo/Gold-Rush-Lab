package io.devyeoooo.Gold_Rush_Lab.mine.repository;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.ActiveMineNotFoundException;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MineJpaAdapterTest {

    @Mock
    private MineJpaRepository mineJpaRepository;

    private MineJpaAdapter mineJpaAdapter;

    @BeforeEach
    void setUp() {
        mineJpaAdapter = new MineJpaAdapter(mineJpaRepository);
    }

    @Test
    void 고갈되지_않은_첫_번째_광산을_반환한다() {
        MineEntity mine = MineEntity.create(100L);
        when(mineJpaRepository.findFirstByRemainingAmountGreaterThanOrderByIdAsc(0))
                .thenReturn(Optional.of(mine));

        MineEntity found = mineJpaAdapter.findFirstNotDepleted();

        assertSame(mine, found);
    }

    @Test
    void 고갈되지_않은_광산이_없으면_예외가_발생한다() {
        when(mineJpaRepository.findFirstByRemainingAmountGreaterThanOrderByIdAsc(0))
                .thenReturn(Optional.empty());

        assertThrows(
                ActiveMineNotFoundException.class,
                mineJpaAdapter::findFirstNotDepleted
        );
    }
}
