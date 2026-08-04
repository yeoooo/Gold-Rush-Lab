package io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID sessionId) {
        super("사용자를 찾을 수 없습니다. sessionId=" + sessionId);
    }
}
