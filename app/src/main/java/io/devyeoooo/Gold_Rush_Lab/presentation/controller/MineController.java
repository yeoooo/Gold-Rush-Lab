package io.devyeoooo.Gold_Rush_Lab.presentation.controller;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.InvalidRequestParameterException;
import io.devyeoooo.Gold_Rush_Lab.mine.service.DistributedMiningService;
import io.devyeoooo.Gold_Rush_Lab.mine.service.MineService;
import io.devyeoooo.Gold_Rush_Lab.presentation.dto.MineRequestDto;
import io.devyeoooo.Gold_Rush_Lab.presentation.dto.comm.ApiResponse;
import io.devyeoooo.Gold_Rush_Lab.presentation.dto.MineDto;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import io.devyeoooo.Gold_Rush_Lab.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MineController {

    private final UserService userService;
    private final MineService mineService;
    private final DistributedMiningService distributedMiningService;

    private final Long TARGET_MINE_ID = 1L;

    @PostMapping("/mines")
    public ApiResponse<MineDto> createMine(
            @RequestParam(name = "amount") Long amount
    ) {
        if (amount < 0) {
            throw new InvalidRequestParameterException("광산의 잔량은 음수일 수 없습니다.");
        }

        Long createdId = mineService.create(amount);
        return ApiResponse.success(MineDto.of(mineService.findById(createdId)));
    }

    @PostMapping("/mine")
    public ApiResponse<MineRequestDto> mine(
            @RequestParam(name = "sessionId") UUID sessionId
            ) {
        distributedMiningService.mine(sessionId, TARGET_MINE_ID, 1L);
        UserEntity foundUser = userService.findBySessionId(sessionId);

        Long totalMinedGold = foundUser.getTotalMinedGold();
        Long remainingAmount = foundUser.getMine().getRemainingAmount();

        return ApiResponse.success(new MineRequestDto(1L, totalMinedGold, remainingAmount));
    }
}
