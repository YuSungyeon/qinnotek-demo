package com.qinnotek.submission.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * application.yml 의 app.* 설정 바인딩.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * 고객이 접속하는 프론트엔드 주소. 제출 링크 = publicUrl + "/s/" + 토큰.
     * 문자로 보내는 링크는 이 값으로 서버가 만든다 (요청에서 받은 주소를 그대로 보내면 피싱 문자에 악용된다).
     */
    private String publicUrl = "http://localhost:5173";

    /** 고객 제출 링크 주소 (문자로 보내는 링크는 항상 이걸로 만든다) */
    public String linkUrl(String token) {
        return publicUrl.replaceAll("/+$", "") + "/s/" + token;
    }

    private final Upload upload = new Upload();
    private final Cors cors = new Cors();
    private final Sms sms = new Sms();
    private final Document document = new Document();

    @Getter
    @Setter
    public static class Upload {
        /** 업로드 파일이 저장될 디렉터리 */
        private String dir = "./uploads";
        /** 이미지 최대 크기 */
        private DataSize maxImageBytes = DataSize.ofMegabytes(20);
        /** 문서 최대 크기 (스캔 PDF 는 이미지보다 큰 경우가 많음) */
        private DataSize maxDocumentBytes = DataSize.ofMegabytes(30);
    }

    @Getter
    @Setter
    public static class Document {
        /** soffice 실행 파일 경로. 비어있으면 자동 탐지 */
        private String sofficeBin = "";
        /** 변환 1건 제한 시간(초) */
        private int convertTimeoutSec = 90;
        /** 동시 변환 수. soffice 인스턴스당 RSS 200~400MB 이므로 기본 1 */
        private int convertConcurrency = 1;
        /** 분류 최저 점수 - 이 아래는 배정 포기(사용자가 직접 선택) */
        private double abstainScore = 0.8;
        /** 분류 확신 최저 점수 - 이 아래는 '확인 필요' 표시 */
        private double lowScore = 2.0;
        /** 1위-2위 점수 차 최저값 - 이 아래는 '확인 필요' 표시 */
        private double lowMargin = 0.8;
    }

    @Getter
    @Setter
    public static class Cors {
        /** 허용할 프론트엔드 오리진(콤마 구분) */
        private String allowedOrigins = "http://localhost:5173";
    }

    @Getter
    @Setter
    public static class Sms {
        /** SMS 발송 활성화 여부 */
        private boolean enabled = false;
        /** Solapi API Key */
        private String apiKey = "";
        /** Solapi API Secret */
        private String apiSecret = "";
        /** 발신번호(Solapi 사전 등록 번호). 비어있으면 관리자 번호를 발신번호로 사용 */
        private String senderNumber = "";
    }
}
