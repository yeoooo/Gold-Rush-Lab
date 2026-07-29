package io.devyeoooo.Gold_Rush_Lab.comm.exception;

public class LockAcquireException extends RuntimeException {
    public LockAcquireException() {
        super("분산 락을 얻는데 실패했습니다.");
    }

}
