package io.devyeoooo.Gold_Rush_Lab.presentation.dto;

import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;

public record MineDto(
        Long mineId,
        Long remainingAmount
) {
    public static MineDto of(MineEntity entity) {
        return new MineDto(entity.getId(), entity.getRemainingAmount());
    }
}
