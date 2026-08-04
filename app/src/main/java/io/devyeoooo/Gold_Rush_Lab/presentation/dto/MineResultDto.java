package io.devyeoooo.Gold_Rush_Lab.presentation.dto;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;

public record MineResultDto(
        Long mineId,
        Long remainingAmount
) {
    public static MineResultDto of(MineEntity entity) {
        return new MineResultDto(entity.getId(), entity.getRemainingAmount());
    }
}
