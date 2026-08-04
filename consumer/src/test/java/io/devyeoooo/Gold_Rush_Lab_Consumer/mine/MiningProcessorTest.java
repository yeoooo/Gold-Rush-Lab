package io.devyeoooo.Gold_Rush_Lab_Consumer.mine;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity.MineEntity;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.MineRepository;
import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningCompletedEvent;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.service.MiningProcessor;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.service.MiningProcessResult;
import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningRequestedEvent;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mining_log.repository.entity.MiningLogEntity;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mining_log.repository.MiningLogRepository;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity.ProcessedMiningEventEntity;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.ProcessedMiningEventRepository;
import io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository.entity.UserEntity;
import io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository.UserRepository;
import io.devyeoooo.Gold_Rush_Lab_Consumer.observability.MiningMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MiningProcessorTest {

    @Mock UserRepository userRepository;
    @Mock MineRepository mineRepository;
    @Mock MiningLogRepository miningLogRepository;
    @Mock ProcessedMiningEventRepository processedEventRepository;
    @Mock MiningMetrics miningMetrics;

    private MiningProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new MiningProcessor(
                userRepository,
                mineRepository,
                miningLogRepository,
                processedEventRepository,
                miningMetrics
        );
    }

    @Test
    void 채굴하고_완료_이벤트를_반환한다() {
        UUID eventId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant requestedAt = Instant.now();
        MiningRequestedEvent requested = new MiningRequestedEvent(
                eventId, sessionId, 10L, 1L, requestedAt
        );
        UserEntity user = mock(UserEntity.class);
        MineEntity mine = mock(MineEntity.class);

        when(processedEventRepository.findById(eventId)).thenReturn(Optional.empty());
        when(userRepository.findBySessionId(sessionId)).thenReturn(Optional.of(user));
        when(user.getMine()).thenReturn(mine);
        when(mine.getId()).thenReturn(10L);
        when(mine.getRemainingAmount()).thenReturn(99L);
        when(mineRepository.findById(10L)).thenReturn(Optional.of(mine));

        MiningProcessResult result = processor.process(requested);
        MiningCompletedEvent completed = result.event();

        assertEquals(false, result.duplicate());
        assertEquals(eventId, completed.eventId());
        assertEquals(10L, completed.mineId());
        assertEquals(99L, completed.remainingAmount());
        verify(mine).mine(1L);
        verify(user).addGold(1L);
        verify(miningLogRepository).save(any(MiningLogEntity.class));
        verify(processedEventRepository).save(any(ProcessedMiningEventEntity.class));
    }

    @Test
    void 이미_처리한_이벤트는_DB를_다시_변경하지_않는다() {
        UUID eventId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant requestedAt = Instant.now();
        MiningRequestedEvent requested = new MiningRequestedEvent(
                eventId, sessionId, 10L, 1L, requestedAt
        );
        MiningCompletedEvent expected = new MiningCompletedEvent(
                eventId, sessionId, 10L, 1L, 99L, requestedAt
        );
        ProcessedMiningEventEntity processed = mock(ProcessedMiningEventEntity.class);

        when(processedEventRepository.findById(eventId)).thenReturn(Optional.of(processed));
        when(processed.toEvent()).thenReturn(expected);

        MiningProcessResult result = processor.process(requested);
        assertEquals(expected, result.event());
        assertEquals(true, result.duplicate());
        verify(miningMetrics).incrementDuplicate();
        verify(userRepository, never()).findBySessionId(any());
        verify(miningLogRepository, never()).save(any());
    }
}
