package io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.kafka;

import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningCompletedEvent;
import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningCompletedPublisher;
import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningRequestedEvent;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.service.MiningProcessor;
import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.service.MiningProcessResult;
import io.devyeoooo.Gold_Rush_Lab_Consumer.observability.MiningFailureClassifier;
import io.devyeoooo.Gold_Rush_Lab_Consumer.observability.MiningFailureType;
import io.devyeoooo.Gold_Rush_Lab_Consumer.observability.MiningMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class MiningEventListener {

    public static final String REQUESTED_TOPIC = "mining-requested";
    private final ObjectMapper objectMapper;
    private final MiningProcessor processor;
    private final MiningCompletedPublisher completedPublisher;
    private final MiningMetrics miningMetrics;
    private final MiningFailureClassifier failureClassifier;
    private final Map<Integer, Long> lastOffsets = new ConcurrentHashMap<>();

    /**
     * Kafka에서 수신한 채굴 요청을 처리하고 완료 이벤트를 발행하는 함수.
     *
     * 1. 수신한 partition과 offset을 메트릭에 기록하고 처리 순서를 확인한다.
     * 2. JSON payload를 채굴 요청 이벤트로 역직렬화한다.
     * 3. 채굴 트랜잭션을 실행하고 신규 처리이면 DB commit 메트릭을 기록한다.
     * 4. 채굴 완료 이벤트를 발행하고 성공 메트릭을 기록한다.
     * 5. 각 단계에서 실패하면 실패 메트릭을 기록한 뒤 예외를 다시 던진다.
     *
     * @param record key, partition, offset 및 JSON payload를 포함한 채굴 요청 레코드
     */
    @KafkaListener(topics = REQUESTED_TOPIC)
    public void consume(ConsumerRecord<String, String> record) {
        miningMetrics.incrementReceived(record.partition());
        trackOrder(record.partition(), record.offset());

        MiningRequestedEvent requested;
        try {
            requested = deserialize(record.value());
        } catch (RuntimeException exception) {
            miningMetrics.incrementFailure(MiningFailureType.DESERIALIZATION);
            throw exception;
        }

        Timer.Sample processingSample = miningMetrics.startTimer();
        MiningProcessResult result;
        try {
            result = processor.process(requested);
            if (!result.duplicate()) {
                miningMetrics.incrementDbCommit();
            }
        } catch (RuntimeException exception) {
            miningMetrics.incrementFailure(failureClassifier.classify(exception));
            throw exception;
        } finally {
            miningMetrics.stopProcessing(processingSample);
        }

        Timer.Sample publishSample = miningMetrics.startTimer();
        try {
            completedPublisher.publish(result.event());
            miningMetrics.incrementSuccess();
        } catch (RuntimeException exception) {
            miningMetrics.incrementFailure(MiningFailureType.KAFKA_PUBLISH);
            throw exception;
        } finally {
            miningMetrics.stopPublish(publishSample);
        }

        log.info(
                "mining_event_processed key={} partition={} offset={} event_id={}",
                record.key(), record.partition(), record.offset(), requested.eventId()
        );
    }

    /**
     * Kafka partition 내부의 메시지 처리 순서를 확인하는 함수.
     *
     * 1. partition별로 마지막에 관측한 offset을 조회한다.
     * 2. 현재 offset이 이전 offset 이하이면 순서 위반 메트릭을 기록한다.
     * 3. 현재까지 관측한 가장 큰 offset을 저장한다.
     *
     * @param partition 레코드가 수신된 Kafka partition
     * @param offset 현재 레코드의 offset
     */
    private void trackOrder(int partition, long offset) {
        lastOffsets.compute(partition, (key, previousOffset) -> {
            if (previousOffset != null && offset <= previousOffset) {
                miningMetrics.incrementOrderViolation(partition);
            }
            return previousOffset == null ? offset : Math.max(previousOffset, offset);
        });
    }

    /**
     * Kafka의 JSON payload를 채굴 요청 이벤트로 변환하는 함수.
     *
     * 1. ObjectMapper를 사용해 payload를 MiningRequestedEvent로 역직렬화한다.
     * 2. 역직렬화에 실패하면 IllegalArgumentException으로 변환해 던진다.
     *
     * @param payload JSON 형식의 채굴 요청 payload
     * @return 역직렬화된 채굴 요청 이벤트
     * @throws IllegalArgumentException payload가 올바른 채굴 요청 형식이 아닌 경우
     */
    private MiningRequestedEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, MiningRequestedEvent.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("채굴 요청 이벤트 역직렬화에 실패했습니다.", exception);
        }
    }
}
