package com.qinnotek.photo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 문서 설정. /swagger-ui.html 에서 확인.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI photoSubmissionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("사진 제출 및 검수 시스템 API")
                        .description("고객 사진 제출 및 관리자 검수(통과/반환) API")
                        .version("v1.0.0"));
    }
}
