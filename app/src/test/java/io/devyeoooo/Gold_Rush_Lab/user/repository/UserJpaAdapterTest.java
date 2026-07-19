package io.devyeoooo.Gold_Rush_Lab.user.repository;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.UserNotFoundException;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserJpaAdapterTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    private UserJpaAdapter userJpaAdapter;

    @BeforeEach
    void setUp() {
        userJpaAdapter = new UserJpaAdapter(userJpaRepository);
    }

    @Test
    void 사용자_식별자로_사용자를_조회한다() {
        UserEntity user = mock(UserEntity.class);
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));

        UserEntity found = userJpaAdapter.findById(1L);

        assertSame(user, found);
    }

    @Test
    void 존재하지_않는_사용자_식별자로_조회하면_예외가_발생한다() {
        when(userJpaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userJpaAdapter.findById(1L));
    }

    @Test
    void 세션_식별자로_사용자를_조회한다() {
        UUID sessionId = UUID.randomUUID();
        UserEntity user = mock(UserEntity.class);
        when(userJpaRepository.findBySessionId(sessionId)).thenReturn(Optional.of(user));

        UserEntity found = userJpaAdapter.findBySessionId(sessionId);

        assertSame(user, found);
    }

    @Test
    void 존재하지_않는_세션_식별자로_조회하면_예외가_발생한다() {
        UUID sessionId = UUID.randomUUID();
        when(userJpaRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userJpaAdapter.findBySessionId(sessionId)
        );
    }
}
