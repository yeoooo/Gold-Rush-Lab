package io.devyeoooo.Gold_Rush_Lab_Consumer.mine.service;

import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningCompletedEvent;
import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningRequestedEvent;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity.MineEntity;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.MineRepository;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mining_log.repository.entity.MiningLogEntity;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mining_log.repository.MiningLogRepository;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity.ProcessedMiningEventEntity;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.ProcessedMiningEventRepository;
import io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository.entity.UserEntity;
import io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository.UserRepository;
import io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository.exception.UserNotFoundException;
import io.devyeoooo.Gold_Rush_Lab_Consumer.observability.MiningMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MiningProcessor {

    private final UserRepository userRepository;
    private final MineRepository mineRepository;
    private final MiningLogRepository miningLogRepository;
    private final ProcessedMiningEventRepository processedEventRepository;
    private final MiningMetrics miningMetrics;

    /**
     * 채굴 요청을 하나의 트랜잭션으로 처리하고 완료 이벤트를 생성하는 함수.
     *
     * 1. eventId로 처리 이력을 조회하고 이미 처리된 요청이면 저장된 결과를 반환한다.
     * 2. sessionId로 사용자를 조회하고 요청한 광산이 사용자의 광산과 일치하는지 검증한다.
     * 3. 광산 잔량과 사용자 골드를 변경하고 채굴 로그를 저장한다.
     * 4. 채굴 완료 이벤트와 처리 이력을 저장한다.
     * 5. 신규 처리 결과를 반환하고, 예외가 발생하면 전체 변경을 롤백한다.
     *
     * @param event 처리할 채굴 요청 이벤트
     * @return 신규 처리 또는 중복 처리 여부와 채굴 완료 이벤트
     * @throws RuntimeException 사용자·광산 검증이나 데이터 변경에 실패해 트랜잭션을 롤백해야 하는 경우
     */
    @Transactional
    public MiningProcessResult process(MiningRequestedEvent event) {
        ProcessedMiningEventEntity processed = processedEventRepository
                .findById(event.eventId())
                .orElse(null);
        if (processed != null) {
            miningMetrics.incrementDuplicate();
            return MiningProcessResult.duplicate(processed.toEvent());
        }

        UserEntity user = userRepository.findBySessionId(event.userSessionId())
                .orElseThrow(() -> new UserNotFoundException(event.userSessionId()));
        if (!user.getMine().getId().equals(event.mineId())) {
            throw new IllegalArgumentException("사용자가 요청한 광산과 이벤트의 광산이 일치하지 않습니다.");
        }
        MineEntity mine = mineRepository.findById(event.mineId())
                .orElseThrow(() -> new IllegalStateException("광산을 찾을 수 없습니다."));

        mine.mine(event.amount());
        user.addGold(event.amount());
        miningLogRepository.save(MiningLogEntity.create(user, mine, event.amount()));

        MiningCompletedEvent completed = new MiningCompletedEvent(
                event.eventId(),
                event.userSessionId(),
                mine.getId(),
                event.amount(),
                mine.getRemainingAmount(),
                event.requestedAt()
        );
        processedEventRepository.save(ProcessedMiningEventEntity.from(completed));
        return MiningProcessResult.completed(completed);
    }
}
