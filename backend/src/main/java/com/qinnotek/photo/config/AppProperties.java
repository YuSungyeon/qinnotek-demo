package com.qinnotek.photo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.yml 의 app.* 설정 바인딩.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Upload upload = new Upload();
    private final Cors cors = new Cors();

    @Getter
    @Setter
    public static class Upload {
        /** 업로드 파일이 저장될 디렉터리 */
        private String dir = "./uploads";
    }

    @Getter
    @Setter
    public static class Cors {
        /** 허용할 프론트엔드 오리진(콤마 구분) */
        private String allowedOrigins = "http://localhost:5173";
    }
}
