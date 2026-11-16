package com.qinnotek.submission.service;

import com.qinnotek.submission.config.AppProperties;
import com.qinnotek.submission.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 업로드 파일 바이러스 검사 - ClamAV 데몬(clamd)에 TCP 로 파일 내용을 보내 결과를 받는다 (INSTREAM 명령).
 * 라이브러리 없이 clamd 프로토콜만 쓴다. clamd 는 서버 PC 에 따로 설치한다 (DEPLOY.md "바이러스 검사").
 *
 * <p>검사를 켰는데 clamd 에 닿지 못하면 업로드를 막는다(fail closed) - 검사 안 된 파일이 NAS 로 가지 않게 한다.
 */
@Slf4j
@Component
public class VirusScanner {

    private final AppProperties.VirusScan props;

    public VirusScanner(AppProperties appProperties) {
        this.props = appProperties.getVirusScan();
    }

    /** 감염이면 400, 검사할 수 없으면 503. 검사가 꺼져 있으면 아무것도 하지 않는다. */
    public void check(InputStream content, String fileName) {
        if (!props.isEnabled()) {
            return;
        }
        String reply;
        try {
            reply = scan(content);
        } catch (IOException e) {
            log.warn("clamd 검사 실패 ({}:{}) - {}", props.getHost(), props.getPort(), e.toString());
            throw unavailable();
        }
        if (reply.endsWith(" OK")) {
            return;
        }
        if (reply.endsWith(" FOUND")) {
            log.warn("바이러스 검출 - 업로드 거부: {} ({})", fileName, reply);
            throw BusinessException.badRequest("보안 검사에서 위험 요소가 발견되어 올릴 수 없습니다.");
        }
        // 예: "INSTREAM size limit exceeded. ERROR" - clamd.conf 의 StreamMaxLength 가 업로드 상한보다 작다
        log.warn("clamd 오류 응답: {} ({})", reply, fileName);
        throw unavailable();
    }

    /** clamd 응답 한 줄을 돌려준다. 예) "stream: OK", "stream: Eicar-Test-Signature FOUND" */
    String scan(InputStream content) throws IOException {
        int timeoutMs = (int) props.getTimeout().toMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(props.getHost(), props.getPort()), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            // 명령 → (4바이트 길이 + 조각) 반복 → 길이 0 으로 끝
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            byte[] buf = new byte[64 * 1024];
            for (int n; (n = content.read(buf)) > 0; ) {
                out.writeInt(n);
                out.write(buf, 0, n);
            }
            out.writeInt(0);
            out.flush();

            // 응답은 \0 으로 끝난다
            InputStream in = socket.getInputStream();
            ByteArrayOutputStream reply = new ByteArrayOutputStream();
            for (int b; (b = in.read()) > 0; ) {
                reply.write(b);
            }
            return reply.toString(StandardCharsets.US_ASCII).trim();
        }
    }

    private static BusinessException unavailable() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                "보안 검사를 할 수 없어 지금은 올릴 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
}
