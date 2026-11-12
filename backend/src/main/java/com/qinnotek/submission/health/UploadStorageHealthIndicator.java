package com.qinnotek.submission.health;

import com.qinnotek.submission.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 헬스체크 항목 "uploadStorage" - 업로드 폴더에 실제로 파일을 쓸 수 있는지 확인한다.
 *
 * <p>왜 필요한가: 이 서비스의 핵심 동작은 고객 파일 저장이다. DB 가 살아 있어도
 * 디스크가 읽기 전용으로 바뀌었거나(EBS 오류), 폴더 권한·마운트가 틀어지면 업로드가 전부 실패한다.
 * diskSpace 항목은 남은 용량만 보므로 이 경우를 잡지 못한다.
 *
 * <p>방법: 임시 폴더(.tmp, 업로드 폴더와 같은 디스크)에 빈 파일을 만들었다 지운다.
 * 실패하면 DOWN → /actuator/health 가 503 을 돌려주고, 외부 모니터링이 알린다.
 * 실패 원인은 AbstractHealthIndicator 가 WARN 로그로 남긴다 (응답에는 세부 내용을 싣지 않는다).
 *
 * <p>비용: 헬스체크 한 번에 빈 파일 생성·삭제 1회. 모니터링 주기(보통 1~5분)에서는 무시할 수준이다.
 */
@Component
@RequiredArgsConstructor
public class UploadStorageHealthIndicator extends AbstractHealthIndicator {

    private final FileStorageService fileStorageService;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        Path dir = fileStorageService.getTmpRoot();
        // 예외가 나면 AbstractHealthIndicator 가 DOWN 으로 바꾸고 WARN 로그를 남긴다
        Path probe = Files.createTempFile(dir, "health-", ".probe");
        Files.delete(probe);
        builder.up();
    }
}
