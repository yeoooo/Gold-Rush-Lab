package io.devyeoooo.Gold_Rush_Lab.comm.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 지원하지_않는_HTTP_메서드는_405를_반환한다() throws Exception {
        mockMvc.perform(post("/test/status"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void 지원하지_않는_미디어_타입은_415를_반환한다() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("body"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void ResponseStatusException의_HTTP_상태를_유지한다() throws Exception {
        mockMvc.perform(get("/test/status"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 광산의_잔량이_부족하면_충돌_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/test/mine/depleted"))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MINE_DEPLETED")));
    }

    @Test
    void 광산을_찾을_수_없으면_찾을_수_없음_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/test/mine/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MINE_NOT_FOUND")));
    }

    @RestController
    @RequestMapping("/test")
    private static class TestController {

        @GetMapping("/status")
        void status() {
            throw new ResponseStatusException(HttpStatusCode.valueOf(403));
        }

        @PostMapping(path = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        void body(@RequestBody String body) {
        }

        @PostMapping("/mine/depleted")
        void mineDepleted() {
            throw new MineDepletedException();
        }

        @GetMapping("/mine/not-found")
        void mineNotFound() {
            throw new MineNotFoundException();
        }
    }
}
