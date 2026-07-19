package io.devyeoooo.Gold_Rush_Lab.presentation.controller;

import io.devyeoooo.Gold_Rush_Lab.comm.exception.ActiveMineNotFoundException;
import io.devyeoooo.Gold_Rush_Lab.comm.exception.GlobalExceptionHandler;
import io.devyeoooo.Gold_Rush_Lab.comm.exception.UserNotFoundException;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import io.devyeoooo.Gold_Rush_Lab.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    static final String SIGNIN_URL = "/v01/user/signin";
    static final String MINE_ID = "10";

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 세션_식별자가_없으면_사용자를_생성한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UserEntity user = createUser(sessionId);
        when(userService.create(10L)).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(user);

        MvcResult result = mockMvc.perform(post(SIGNIN_URL)
                        .param("mineId", MINE_ID))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains(sessionId.toString()));
        verify(userService).create(10L);
        verify(userService).findById(1L);
    }

    @Test
    void 같은_광산으로_여러_사용자를_생성한다() throws Exception {
        UUID firstSessionId = UUID.randomUUID();
        UUID secondSessionId = UUID.randomUUID();
        UserEntity firstUser = createUser(firstSessionId);
        UserEntity secondUser = createUser(secondSessionId);
        when(userService.create(10L)).thenReturn(1L, 2L);
        when(userService.findById(1L)).thenReturn(firstUser);
        when(userService.findById(2L)).thenReturn(secondUser);

        mockMvc.perform(post(SIGNIN_URL)
                        .param("mineId", MINE_ID))
                .andExpect(status().isOk());

        mockMvc.perform(post(SIGNIN_URL)
                        .param("mineId", MINE_ID))
                .andExpect(status().isOk());

        verify(userService, times(2)).create(10L);
        verify(userService).findById(1L);
        verify(userService).findById(2L);
    }

    @Test
    void 세션_식별자가_있으면_기존_사용자를_조회한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UserEntity user = createUser(sessionId);
        when(userService.findBySessionId(sessionId)).thenReturn(user);

        MvcResult result = mockMvc.perform(post(SIGNIN_URL)
                        .param("sessionId", sessionId.toString())
                        .param("mineId", MINE_ID))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains(sessionId.toString()));
        verify(userService).findBySessionId(sessionId);
        verify(userService, never()).create(10L);
    }

    @Test
    void 세션_식별자_형식이_잘못되면_잘못된_요청을_반환한다() throws Exception {
        MvcResult result = mockMvc.perform(post(SIGNIN_URL)
                        .param("sessionId", "잘못된-세션-식별자")
                        .param("mineId", MINE_ID))
                .andExpect(status().isBadRequest())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("INVALID_REQUEST_PARAMETER"));
    }

    @Test
    void 광산_식별자가_없으면_잘못된_요청을_반환한다() throws Exception {
        mockMvc.perform(post(SIGNIN_URL))
                .andExpect(status().isBadRequest());

        verify(userService, never()).create(10L);
    }

    @Test
    void 사용자를_찾을_수_없으면_찾을_수_없음_응답을_반환한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(userService.findBySessionId(sessionId)).thenThrow(new UserNotFoundException());

        MvcResult result = mockMvc.perform(post(SIGNIN_URL)
                        .param("sessionId", sessionId.toString())
                        .param("mineId", MINE_ID))
                .andExpect(status().isNotFound())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("USER_NOT_FOUND"));
    }

    @Test
    void 활성_광산이_없으면_충돌_응답을_반환한다() throws Exception {
        when(userService.create(10L)).thenThrow(new ActiveMineNotFoundException());

        MvcResult result = mockMvc.perform(post(SIGNIN_URL)
                        .param("mineId", MINE_ID))
                .andExpect(status().isConflict())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("ACTIVE_MINE_NOT_FOUND"));
    }

    @Test
    void 예상하지_못한_예외가_발생하면_서버_오류를_반환한다() throws Exception {
        when(userService.create(10L)).thenThrow(new RuntimeException("노출되면 안 되는 메시지"));

        MvcResult result = mockMvc.perform(post(SIGNIN_URL)
                        .param("mineId", MINE_ID))
                .andExpect(status().isInternalServerError())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("INTERNAL_SERVER_ERROR"));
        assertFalse(response.contains("노출되면 안 되는 메시지"));
    }

    private UserEntity createUser(UUID sessionId) {
        UserEntity user = mock(UserEntity.class);
        when(user.getSessionId()).thenReturn(sessionId);
        return user;
    }
}
