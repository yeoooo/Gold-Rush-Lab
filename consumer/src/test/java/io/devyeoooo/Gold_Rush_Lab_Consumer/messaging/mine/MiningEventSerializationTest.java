package io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiningEventSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 요청_이벤트의_시간을_JSON으로_직렬화하고_역직렬화한다() throws Exception {
        MiningRequestedEvent expected = new MiningRequestedEvent(
                UUID.randomUUID(), UUID.randomUUID(), 10L, 1L, Instant.now()
        );

        String payload = objectMapper.writeValueAsString(expected);
        MiningRequestedEvent actual = objectMapper.readValue(payload, MiningRequestedEvent.class);

        assertEquals(expected, actual);
    }
}
