package io.devyeoooo.Gold_Rush_Lab.mine.repository.entity;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.MineDepletedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MineEntityTest {

    @Test
    void 잔량이_양수인_광산을_생성한다() {
        MineEntity mine = MineEntity.create(100L);

        assertEquals(100L, mine.getRemainingAmount());
    }

    @Test
    void 잔량이_영인_광산을_생성할_수_있다() {
        assertDoesNotThrow(() -> MineEntity.create(0L));
    }

    @Test
    void 잔량이_음수이면_예외가_발생한다() {
        assertThrows(IllegalArgumentException.class, () -> MineEntity.create(-1L));
    }

    @Test
    void 잔량이_없으면_예외가_발생한다() {
        assertThrows(IllegalArgumentException.class, () -> MineEntity.create(null));
    }

    @Test
    void 광산의_잔량을_채굴량만큼_감소시킨다() {
        MineEntity mine = MineEntity.create(100L);

        mine.mine(1L);

        assertEquals(99L, mine.getRemainingAmount());
    }

    @Test
    void 잔량을_모두_채굴할_수_있다() {
        MineEntity mine = MineEntity.create(1L);

        mine.mine(1L);

        assertEquals(0L, mine.getRemainingAmount());
    }

    @Test
    void 채굴량이_잔량을_넘으면_예외가_발생하고_잔량이_유지된다() {
        MineEntity mine = MineEntity.create(1L);

        assertThrows(MineDepletedException.class, () -> mine.mine(2L));
        assertEquals(1L, mine.getRemainingAmount());
    }

    @Test
    void 채굴량이_영이면_예외가_발생한다() {
        MineEntity mine = MineEntity.create(1L);

        assertThrows(IllegalArgumentException.class, () -> mine.mine(0L));
    }

    @Test
    void 채굴량이_음수이면_예외가_발생한다() {
        MineEntity mine = MineEntity.create(1L);

        assertThrows(IllegalArgumentException.class, () -> mine.mine(-1L));
    }

    @Test
    void 채굴량이_없으면_예외가_발생한다() {
        MineEntity mine = MineEntity.create(1L);

        assertThrows(IllegalArgumentException.class, () -> mine.mine(null));
    }
}
