package io.devyeoooo.Gold_Rush_Lab.comm.exception;

public class ActiveMineNotFoundException extends RuntimeException {

    public ActiveMineNotFoundException() {
        super("활성화된 광산이 없습니다.");
    }
}
