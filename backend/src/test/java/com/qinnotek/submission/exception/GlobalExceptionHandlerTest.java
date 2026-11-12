package com.qinnotek.submission.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 모니터링 기준: 클라이언트 잘못은 4xx, 서버 문제만 5xx. 500 응답에는 내부 메시지를 싣지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @TestConfiguration
    static class Boom {
        @Bean
        BoomController boomController() {
            return new BoomController();
        }
    }

    @RestController
    static class BoomController {
        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("db password=secret");
        }

        @GetMapping("/test/abort")
        String abort() throws IOException {
            throw new IOException("Broken pipe");
        }
    }

    @Test
    void 없는_주소는_404() throws Exception {
        mvc.perform(post("/api/customer/lookup").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("요청한 주소를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/api/customer/lookup"));
    }

    @Test
    void 지원하지_않는_메서드는_405() throws Exception {
        mvc.perform(post("/api/admin/companies/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("지원하지 않는 요청 방식입니다."));
    }

    @Test
    void 경로_값_형식이_틀리면_400() throws Exception {
        mvc.perform(get("/api/admin/companies/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 값 형식이 올바르지 않습니다: id"));
    }

    @Test
    void 본문을_읽을_수_없으면_400() throws Exception {
        mvc.perform(post("/api/admin/companies").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다."));
    }

    @Test
    void 검증_실패는_400_필드_메시지() throws Exception {
        mvc.perform(post("/api/admin/companies").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("기업명을 입력해주세요."));
    }

    @Test
    void 비즈니스_예외는_정한_상태_그대로() throws Exception {
        mvc.perform(get("/api/customer/links/no-such-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("유효하지 않은 링크입니다."));
    }

    @Test
    void 예상하지_못한_예외는_500_내부_메시지는_숨김() throws Exception {
        String body = mvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.SERVER_ERROR_MESSAGE))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("secret");
    }

    @Test
    void 클라이언트_연결_끊김은_오류_응답을_쓰지_않는다() throws Exception {
        mvc.perform(get("/test/abort"))
                .andExpect(content().string(""));
    }

    @Test
    void 연결_끊김_판별() {
        assertThat(GlobalExceptionHandler.isClientAbort(new RuntimeException(new IOException("Connection reset by peer"))))
                .isTrue();
        assertThat(GlobalExceptionHandler.isClientAbort(new IOException("disk full"))).isFalse();
    }
}
