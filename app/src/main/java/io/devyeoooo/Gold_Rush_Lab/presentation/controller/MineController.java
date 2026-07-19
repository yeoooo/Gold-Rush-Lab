package io.devyeoooo.Gold_Rush_Lab.presentation.controller;

import io.devyeoooo.Gold_Rush_Lab.mine.service.MineService;
import io.devyeoooo.Gold_Rush_Lab.presentation.dto.MineRequestDto;
import io.devyeoooo.Gold_Rush_Lab.presentation.dto.comm.ApiResponse;
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

    @PostMapping("/v01/mine")
    public ApiResponse<MineRequestDto> mine(
            @RequestParam(name = "sessionId") UUID sessionId
            ) {
        mineService.mine(sessionId, 1L);
        UserEntity foundUser = userService.findBySessionId(sessionId);

        Long totalMinedGold = foundUser.getTotalMinedGold();
        Long remainingAmount = foundUser.getMine().getRemainingAmount();

        return ApiResponse.success(new MineRequestDto(1L, totalMinedGold, remainingAmount));
    }
}
