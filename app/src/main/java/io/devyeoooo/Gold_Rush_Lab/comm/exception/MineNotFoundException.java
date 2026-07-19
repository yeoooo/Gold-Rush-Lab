package io.devyeoooo.Gold_Rush_Lab.comm.exception;

public class MineNotFoundException extends RuntimeException {

    public MineNotFoundException() {
        super("광산을 찾을 수 없습니다.");
    }
}
