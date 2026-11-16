package com.qinnotek.submission.service;

import com.qinnotek.submission.config.AppProperties;
import com.qinnotek.submission.domain.SubmissionItem;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * NAS 보관함 - 기업별 폴더에 제출 파일을 사람이 알아볼 이름으로 복사해 둔다.
 * <pre>
 *   {app.archive.dir}/{기업명}/{요청자료명}.{확장자}
 * </pre>
 *
 * <ul>
 *   <li>앱이 쓰는 원본 저장소(app.upload.dir, UUID 이름)는 그대로다. 여기는 사람이 보는 사본이다.</li>
 *   <li>기업을 만들면 폴더를 만들고, 이름을 바꾸면 폴더 이름도 바꾼다.</li>
 *   <li>고객이 파일을 올리는 즉시(바이러스 검사 통과 후) 복사한다. 다시 올리면 덮어쓴다.</li>
 *   <li>기업 삭제·요청 자료 해제 때는 지우지 않는다 (보관용).</li>
 *   <li>NAS 가 느리거나 꺼져 있어도 고객 업로드가 막히지 않게 별도 스레드에서 처리한다. 실패는 WARN 로그만 남는다.</li>
 * </ul>
 * app.archive.dir 이 비어 있으면 아무것도 하지 않는다.
 */
@Slf4j
@Service
public class ArchiveService {

    private final Path root;
    private final FileStorageService fileStorageService;
    // ponytail: 스레드 1개 = 요청 순서대로 처리(재업로드가 이전 파일로 덮이지 않게). 실패 재시도 없음 - 필요하면 재동기화 기능 추가
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "nas-archive");
        t.setDaemon(true);
        return t;
    });

    public ArchiveService(AppProperties appProperties, FileStorageService fileStorageService) {
        String dir = appProperties.getArchive().getDir();
        this.root = (dir == null || dir.isBlank()) ? null : Paths.get(dir).toAbsolutePath().normalize();
        this.fileStorageService = fileStorageService;
        if (root != null) {
            log.info("NAS 보관 폴더: {}", root);
        }
    }

    public void createCompanyFolder(String companyName) {
        run("폴더 생성: " + companyName, () -> Files.createDirectories(companyDir(companyName)));
    }

    public void renameCompanyFolder(String oldName, String newName) {
        run("폴더 이름 변경: " + oldName + " → " + newName, () -> {
            Path from = companyDir(oldName);
            Path to = companyDir(newName);
            if (Files.isDirectory(from) && !Files.exists(to)) {
                Files.move(from, to);
            }
        });
    }

    /** 항목에 붙은 파일을 기업 폴더로 복사한다. 지연 로딩 때문에 트랜잭션 안에서 호출한다. */
    public void copy(SubmissionItem item) {
        if (root == null || !item.hasFile()) {
            return;
        }
        // 엔티티는 다른 스레드로 넘기지 않는다 - 필요한 값만 지금 꺼낸다
        String company = item.getCompany().getName();
        String base = safe(item.getRequirement().getName());
        String stored = item.getStoredFileName();
        Path source = fileStorageService.pathOf(stored);
        String ext = stored.contains(".") ? stored.substring(stored.lastIndexOf('.')) : "";

        run("복사: " + company + "/" + base + ext, () -> {
            Path dir = Files.createDirectories(companyDir(company));
            // 확장자가 다른 이전 제출본(jpg → png 재업로드)은 지운다
            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : files.filter(p -> isCopyOf(p, base)).toList()) {
                    Files.deleteIfExists(p);
                }
            }
            // 임시 이름으로 다 쓴 뒤 바꾼다 - 복사 중 끊겨도 반쪽 파일이 제 이름으로 남지 않는다
            Path part = dir.resolve("." + base + ext + ".part");
            Files.copy(source, part, StandardCopyOption.REPLACE_EXISTING);
            Files.move(part, dir.resolve(base + ext), StandardCopyOption.REPLACE_EXISTING);
        });
    }

    /** 대기 중인 작업이 모두 끝날 때까지 기다린다 (테스트용). */
    void awaitIdle() throws Exception {
        worker.submit(() -> { }).get(30, TimeUnit.SECONDS);
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        // 재배포로 꺼질 때 남은 복사를 마저 끝낸다
        worker.shutdown();
        if (!worker.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("NAS 보관: 끝내지 못한 작업이 있습니다");
        }
    }

    private interface IoTask {
        void run() throws IOException;
    }

    private void run(String what, IoTask task) {
        if (root == null) {
            return;
        }
        worker.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.warn("NAS 보관 실패 - {}: {}", what, e.toString());
            }
        });
    }

    private Path companyDir(String companyName) {
        return root.resolve(safe(companyName));
    }

    /** "{base}.{확장자}" 꼴인지 (base 뒤에 점이 하나만 있는 파일) */
    private static boolean isCopyOf(Path p, String base) {
        String name = p.getFileName().toString();
        return name.startsWith(base + ".") && name.indexOf('.', base.length() + 1) < 0;
    }

    /**
     * 폴더/파일 이름으로 쓸 수 있게 바꾼다. 경로 구분자·윈도우 금지 문자는 '_',
     * 앞뒤 점·공백은 없앤다("..", ".숨김", 윈도우에서 끝 점이 사라지는 문제 방지).
     */
    static String safe(String name) {
        String s = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .trim()
                .replaceAll("^[. ]+|[. ]+$", "");
        return s.isEmpty() ? "_" : s;
    }
}
