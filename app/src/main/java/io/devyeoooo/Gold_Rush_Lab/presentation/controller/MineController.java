package io.devyeoooo.Gold_Rush_Lab.presentation.controller;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.InvalidRequestParameterException;
import io.devyeoooo.Gold_Rush_Lab.mine.service.MineService;
import io.devyeoooo.Gold_Rush_Lab.messaging.mine.MiningRequestedEvent;
import io.devyeoooo.Gold_Rush_Lab.presentation.dto.comm.ApiResponse;
import io.devyeoooo.Gold_Rush_Lab.presentation.dto.MineResultDto;
import io.devyeoooo.Gold_Rush_Lab.presentation.dto.MiningAcceptedDto;
import io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine.MineSseEmitterManager;
import io.devyeoooo.Gold_Rush_Lab.presentation.sse.mine.MiningRequestedPublisher;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import io.devyeoooo.Gold_Rush_Lab.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MineController {

    private final MineService mineService;
    private final UserService userService;
    private final MineSseEmitterManager emitterManager;
    private final MiningRequestedPublisher miningRequestedPublisher;


    @GetMapping(
            value = "/events/{sessionId}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter subscribe(
            @PathVariable UUID sessionId
    ) {
        return emitterManager.connect(sessionId);
    }

    @PostMapping("/mines")
    public ApiResponse<MineResultDto> createMine(
            @RequestParam(name = "amount") Long amount
    ) {
        if (amount < 0) {
            throw new InvalidRequestParameterException("광산의 잔량은 음수일 수 없습니다.");
        }

        Long createdId = mineService.create(amount);
        return ApiResponse.success(MineResultDto.of(mineService.findById(createdId)));
    }

    @PostMapping("/mine")
    public ResponseEntity<ApiResponse<MiningAcceptedDto>> mine(
            @RequestParam(name = "sessionId") UUID sessionId
            ) {
        UserEntity user = userService.findBySessionId(sessionId);
        MiningRequestedEvent event = new MiningRequestedEvent(
                UUID.randomUUID(),
                sessionId,
                user.getMine().getId(),
                1L,
                Instant.now()
        );
        miningRequestedPublisher.publish(event);
        return ResponseEntity.accepted().body(
                ApiResponse.success(new MiningAcceptedDto(event.eventId()))
        );
    }
}
