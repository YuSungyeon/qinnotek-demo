package com.qinnotek.submission.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 외부 모니터링과 배포 워크플로가 보는 /actuator/health 의 약속을 지킨다.
 * 앱 전체를 띄우므로 Flyway 마이그레이션 → ddl-auto:validate 통과까지 함께 확인된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 정상이면_200_UP_이고_세부_내용은_숨긴다() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void liveness_readiness_도_열려_있다() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void health_외_액추에이터_엔드포인트는_노출하지_않는다() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
    }

    @Test
    void 마이그레이션이_모두_성공적으로_적용된다() {
        List<Boolean> success = jdbc.queryForList(
                "select \"success\" from \"flyway_schema_history\" where \"version\" is not null", Boolean.class);
        assertThat(success).isNotEmpty().allMatch(Boolean.TRUE::equals);
    }
}
