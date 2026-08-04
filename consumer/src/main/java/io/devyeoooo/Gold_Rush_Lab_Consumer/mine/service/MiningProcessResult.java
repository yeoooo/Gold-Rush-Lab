package io.devyeoooo.Gold_Rush_Lab_Consumer.mine.service;

import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningCompletedEvent;

public record MiningProcessResult(
        MiningCompletedEvent event,
        boolean duplicate
) {
    public static MiningProcessResult completed(MiningCompletedEvent event) {
        return new MiningProcessResult(event, false);
    }

    public static MiningProcessResult duplicate(MiningCompletedEvent event) {
        return new MiningProcessResult(event, true);
    }
}
