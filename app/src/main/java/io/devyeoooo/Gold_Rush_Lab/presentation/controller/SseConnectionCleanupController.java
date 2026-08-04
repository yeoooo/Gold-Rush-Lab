package io.devyeoooo.Gold_Rush_Lab.presentation.controller;

import io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine.SseCleanupPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SseConnectionCleanupController {

    private final SseCleanupPublisher cleanupPublisher;

    @PostMapping("/internal/sse/connections/cleanup")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void cleanup() {
        cleanupPublisher.publish();
    }
}
