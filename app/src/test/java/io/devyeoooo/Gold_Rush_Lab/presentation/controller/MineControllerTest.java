package io.devyeoooo.Gold_Rush_Lab.presentation.controller;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.GlobalExceptionHandler;
import io.devyeoooo.Gold_Rush_Lab.comm.exception.MineDepletedException;
import io.devyeoooo.Gold_Rush_Lab.comm.exception.UserNotFoundException;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import io.devyeoooo.Gold_Rush_Lab.mine.service.MineService;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import io.devyeoooo.Gold_Rush_Lab.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class MineControllerTest {

    private static final String MINE_URL = "/v01/mine";
    private static final String CREATE_MINE_URL = "/mines";

    @Mock
    private UserService userService;

    @Mock
    private MineService mineService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new MineController(userService, mineService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 광산을_생성하고_생성된_광산을_반환한다() throws Exception {
        MineEntity mine = mock(MineEntity.class);
        when(mineService.create(100L)).thenReturn(1L);
        when(mineService.findById(1L)).thenReturn(mine);
        when(mine.getId()).thenReturn(1L);
        when(mine.getRemainingAmount()).thenReturn(100L);

        mockMvc.perform(post(CREATE_MINE_URL).param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mineId").value(1))
                .andExpect(jsonPath("$.data.remainingAmount").value(100));

        verify(mineService).create(100L);
        verify(mineService).findById(1L);
    }

    @Test
    void 광산_잔량이_음수면_잘못된_요청을_반환한다() throws Exception {
        mockMvc.perform(post(CREATE_MINE_URL).param("amount", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "INVALID_REQUEST_PARAMETER"
                )));

        verify(mineService, never()).create(-1L);
    }

    @Test
    void 세션_식별자의_사용자가_금을_하나_채굴한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        MineEntity mine = mock(MineEntity.class);
        UserEntity user = mock(UserEntity.class);
        when(userService.findBySessionId(sessionId)).thenReturn(user);
        when(user.getTotalMinedGold()).thenReturn(1L);
        when(user.getMine()).thenReturn(mine);
        when(mine.getRemainingAmount()).thenReturn(99L);

        mockMvc.perform(post(MINE_URL).param("sessionId", sessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.earned").value(1))
                .andExpect(jsonPath("$.data.totalGold").value(1))
                .andExpect(jsonPath("$.data.remained").value(99));

        verify(mineService).mine(sessionId, 1L);
        verify(userService).findBySessionId(sessionId);
    }

    @Test
    void 세션_식별자가_없으면_잘못된_요청을_반환한다() throws Exception {
        mockMvc.perform(post(MINE_URL))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 세션_식별자_형식이_잘못되면_잘못된_요청을_반환한다() throws Exception {
        mockMvc.perform(post(MINE_URL).param("sessionId", "잘못된-세션-식별자"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "INVALID_REQUEST_PARAMETER"
                )));
    }

    @Test
    void 사용자를_찾을_수_없으면_찾을_수_없음_응답을_반환한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        doThrow(new UserNotFoundException()).when(mineService).mine(sessionId, 1L);

        mockMvc.perform(post(MINE_URL).param("sessionId", sessionId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("USER_NOT_FOUND")));
    }

    @Test
    void 광산의_잔량이_부족하면_충돌_응답을_반환한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        doThrow(new MineDepletedException()).when(mineService).mine(sessionId, 1L);

        mockMvc.perform(post(MINE_URL).param("sessionId", sessionId.toString()))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MINE_DEPLETED")));
    }

    @Test
    void 예상하지_못한_예외가_발생하면_서버_오류를_반환한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        doThrow(new RuntimeException("노출되면 안 되는 메시지"))
                .when(mineService).mine(sessionId, 1L);

        mockMvc.perform(post(MINE_URL).param("sessionId", sessionId.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "INTERNAL_SERVER_ERROR"
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("노출되면 안 되는 메시지")
                )));
    }
}
