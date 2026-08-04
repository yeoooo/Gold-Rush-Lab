package io.devyeoooo.Gold_Rush_Lab_Consumer.observability;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.exception.MineDepletedException;
import io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiningFailureClassifierTest {

    private final MiningFailureClassifier classifier = new MiningFailureClassifier();

    @Test
    void 비즈니스와_DB_실패를_분류한다() {
        assertEquals(MiningFailureType.USER_NOT_FOUND,
                classifier.classify(new UserNotFoundException(UUID.randomUUID())));
        assertEquals(MiningFailureType.MINE_DEPLETED,
                classifier.classify(new MineDepletedException(1L)));
        assertEquals(MiningFailureType.DATABASE,
                classifier.classify(new DataAccessResourceFailureException("database")));
    }
}
