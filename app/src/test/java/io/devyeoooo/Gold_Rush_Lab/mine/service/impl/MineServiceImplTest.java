package io.devyeoooo.Gold_Rush_Lab.mine.service.impl;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.MineRepository;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import io.devyeoooo.Gold_Rush_Lab.mining_log.repository.MiningLogRepository;
import io.devyeoooo.Gold_Rush_Lab.user.repository.UserRepository;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MineServiceImplTest {

    @Mock
    private MineRepository mineRepository;

    @Mock
    private MiningLogRepository miningLogRepository;

    @Mock
    private UserRepository userRepository;

    private MineServiceImpl mineService;

    @BeforeEach
    void setUp() {
        mineService = new MineServiceImpl(mineRepository, userRepository, miningLogRepository);
    }

    @Test
    void 세션_식별자의_사용자가_소속된_광산에서_금을_하나_채굴한다() {
        MineEntity mine = MineEntity.create(100L);
        UserEntity user = UserEntity.create(mine);
        UUID sessionId = user.getSessionId();
        when(userRepository.findBySessionId(sessionId)).thenReturn(user);

        mineService.mine(sessionId, 1L);

        verify(userRepository).findBySessionId(sessionId);
        assertEquals(99L, mine.getRemainingAmount());
        assertEquals(1L, user.getTotalMinedGold());
    }
}
