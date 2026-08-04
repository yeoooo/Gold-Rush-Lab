package io.devyeoooo.Gold_Rush_Lab_Consumer;

import org.junit.jupiter.api.Test;

import io.devyeoooo.Gold_Rush_Lab_Consumer.messaging.mine.MiningRequestedEvent;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GoldRushLabConsumerApplicationTests {

	@Test
	void 채굴량은_양수여야_한다() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new MiningRequestedEvent(
						UUID.randomUUID(), UUID.randomUUID(), 1L, 0L, Instant.now()
				)
		);
	}

}
