package io.devyeoooo.Gold_Rush_Lab.comm.exception;

public class MineDepletedException extends RuntimeException {

    public MineDepletedException() {
        super("광산의 잔량이 부족합니다.");
    }
}
