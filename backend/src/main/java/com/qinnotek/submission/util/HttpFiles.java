package com.qinnotek.submission.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 다운로드 응답 헤더 유틸. */
public final class HttpFiles {

    private HttpFiles() {
    }

    /**
     * RFC 5987 방식으로 한글 파일명을 안전하게 인코딩한다.
     * 개행/따옴표는 헤더 인젝션을 막기 위해 미리 제거한다.
     */
    public static String contentDisposition(String disposition, String fileName) {
        String safe = (fileName == null || fileName.isBlank() ? "download" : fileName)
                .replaceAll("[\\r\\n\"\\\\]", "_");
        String encoded = URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");
        return disposition + "; filename*=UTF-8''" + encoded;
    }
}
