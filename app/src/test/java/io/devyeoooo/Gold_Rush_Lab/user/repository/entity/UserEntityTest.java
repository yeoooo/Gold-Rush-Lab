package io.devyeoooo.Gold_Rush_Lab.user.repository.entity;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserEntityTest {

    @Test
    void 광산에_소속된_사용자를_생성한다() {
        MineEntity mine = MineEntity.create(100L);

        UserEntity user = UserEntity.create(mine);

        assertSame(mine, user.getMine());
        assertEquals(0L, user.getTotalMinedGold());
        assertNotNull(user.getSessionId());
    }

    @Test
    void 광산이_없으면_사용자를_생성할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> UserEntity.create(null));
    }
}
