package io.devyeoooo.Gold_Rush_Lab.presentation.controller;

import io.devyeoooo.Gold_Rush_Lab.presentation.dto.SigninDto;
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
public class UserController {

    private final UserService userService;

    @PostMapping("/v01/user/signin")
    public ApiResponse<SigninDto> signin(
            @RequestParam(required = false) UUID sessionId,
            @RequestParam Long mineId
    ) {
        UserEntity target;

        // 입력받은 세션 ID가 존재하면 조회
        if (sessionId != null) {
            target = userService.findBySessionId(sessionId);
        // 입력받은 세션 ID가 존재하지 않으면 생성
        }else{
            Long createdId = userService.create(mineId);
            target = userService.findById(createdId);
        }
        return ApiResponse.success(new SigninDto(target.getSessionId().toString()));
    }
}
