package io.devyeoooo.Gold_Rush_Lab_Consumer.mine.exception;

public class MineDepletedException extends RuntimeException {
    public MineDepletedException(Long mineId) {
        super("광산 잔량이 부족합니다. mineId=" + mineId);
    }
}
