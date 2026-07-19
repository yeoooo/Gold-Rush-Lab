package io.devyeoooo.Gold_Rush_Lab.user.service.impl;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.ActiveMineNotFoundException;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.MineRepository;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import io.devyeoooo.Gold_Rush_Lab.user.repository.UserRepository;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MineRepository mineRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, mineRepository);
    }

    @Test
    void 활성_광산으로_사용자를_저장하고_식별자를_반환한다() {
        MineEntity mine = MineEntity.create(100L);
        UserEntity savedUser = mock(UserEntity.class);
        when(savedUser.getId()).thenReturn(1L);
        when(mineRepository.findFirstNotDepleted()).thenReturn(mine);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        Long createdId = userService.create();

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals(1L, createdId);
        assertSame(mine, captor.getValue().getMine());
        assertEquals(0L, captor.getValue().getTotalMinedGold());
    }

    @Test
    void 활성_광산이_없으면_사용자를_저장하지_않는다() {
        when(mineRepository.findFirstNotDepleted())
                .thenThrow(new ActiveMineNotFoundException());

        assertThrows(ActiveMineNotFoundException.class, userService::create);
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void 지정한_활성_광산으로_사용자를_생성한다() {
        MineEntity mine = MineEntity.create(100L);
        UserEntity savedUser = mock(UserEntity.class);
        when(savedUser.getId()).thenReturn(1L);
        when(mineRepository.findById(10L)).thenReturn(mine);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        Long createdId = userService.create(10L);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals(1L, createdId);
        assertSame(mine, captor.getValue().getMine());
    }

    @Test
    void 지정한_광산이_고갈되었으면_사용자를_저장하지_않는다() {
        MineEntity mine = MineEntity.create(0L);
        when(mineRepository.findById(10L)).thenReturn(mine);

        assertThrows(ActiveMineNotFoundException.class, () -> userService.create(10L));

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void 세션_식별자로_사용자를_조회한다() {
        UUID sessionId = UUID.randomUUID();
        UserEntity user = mock(UserEntity.class);
        when(userRepository.findBySessionId(sessionId)).thenReturn(user);

        UserEntity found = userService.findBySessionId(sessionId);

        assertSame(user, found);
        verify(userRepository).findBySessionId(sessionId);
    }
}
