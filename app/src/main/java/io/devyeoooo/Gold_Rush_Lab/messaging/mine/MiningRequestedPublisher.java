package io.devyeoooo.Gold_Rush_Lab.messaging.mine;

public interface MiningRequestedPublisher {

    void publish(MiningRequestedEvent event);
}
