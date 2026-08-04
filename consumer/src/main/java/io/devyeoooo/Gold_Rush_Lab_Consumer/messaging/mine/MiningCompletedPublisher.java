package io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine;

public interface MiningCompletedPublisher {

    /**
     * 처리된 채굴 결과를 후속 메시징 채널에 발행하는 함수.
     *
     * 1. 채굴 완료 이벤트를 구현체가 사용하는 메시지 형식으로 변환한다.
     * 2. 변환한 메시지를 후속 메시징 채널에 발행한다.
     * 3. 발행에 실패하면 예외를 호출자에게 전달한다.
     *
     * @param event 발행할 채굴 완료 이벤트
     */
    void publish(MiningCompletedEvent event);
}
