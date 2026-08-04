package io.devyeoooo.Gold_Rush_Lab.presentation.controller;

import io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine.SseCleanupPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SseConnectionCleanupControllerTest {

    @Test
    void 전체_SSE_연결_cleanup을_요청한다() throws Exception {
        SseCleanupPublisher cleanupPublisher =
                mock(SseCleanupPublisher.class);
        MockMvc mockMvc = standaloneSetup(
                new SseConnectionCleanupController(cleanupPublisher)
        ).build();

        mockMvc.perform(post("/internal/sse/connections/cleanup"))
                .andExpect(status().isAccepted());

        verify(cleanupPublisher).publish();
    }
}
