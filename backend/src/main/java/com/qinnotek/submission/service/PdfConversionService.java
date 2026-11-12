package com.qinnotek.submission.service;

import com.qinnotek.submission.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * LibreOffice headless 를 이용한 PDF 변환.
 *
 * <p>soffice 는 인스턴스당 200~400MB 를 쓰므로 세마포어로 동시 실행을 제한한다.
 * LibreOffice 가 없는 개발 환경에서도 앱은 정상 기동하며, 변환만 실패로 강등된다.
 */
@Slf4j
@Service
public class PdfConversionService {

    /** soffice 실행 파일 자동 탐지 후보 */
    private static final List<String> BIN_CANDIDATES = List.of(
            "/usr/bin/soffice",
            "/usr/local/bin/soffice",
            "/opt/libreoffice/program/soffice",
            "/Applications/LibreOffice.app/Contents/MacOS/soffice"
    );

    private final Semaphore gate;
    private final Path tmpRoot;
    private final int timeoutSec;
    private final String configuredBin;

    private volatile String sofficeBin;
    private volatile boolean available;

    /**
     * @param pdfPath   생성된(또는 원본 그대로인) PDF 경로. 실패 시 null
     * @param converted 실제 변환이 일어났는지 (원본이 이미 PDF 면 false)
     * @param message   실패 사유. 성공 시 null
     */
    public record Result(Path pdfPath, boolean converted, String message) {
        public boolean success() {
            return pdfPath != null;
        }

        static Result fail(String message) {
            return new Result(null, false, message);
        }
    }

    public PdfConversionService(AppProperties appProperties) {
        var doc = appProperties.getDocument();
        this.configuredBin = doc.getSofficeBin();
        this.timeoutSec = doc.getConvertTimeoutSec();
        this.gate = new Semaphore(Math.max(1, doc.getConvertConcurrency()));
        this.tmpRoot = Paths.get(appProperties.getUpload().getDir())
                .toAbsolutePath().normalize().resolve(".tmp");
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(tmpRoot);
        } catch (IOException e) {
            log.warn("임시 디렉터리를 만들 수 없습니다: {}", tmpRoot, e);
        }
        this.sofficeBin = detectBinary();
        this.available = sofficeBin != null && probe(sofficeBin);
        if (available) {
            log.info("PDF 변환기 준비 완료: {}", sofficeBin);
        } else {
            log.warn("LibreOffice(soffice) 를 찾을 수 없습니다. 문서는 원본만 저장되고 PDF 사본은 만들어지지 않습니다. "
                    + "설치: deploy/libreoffice-setup.sh (Ubuntu) / brew install --cask libreoffice (macOS)");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * 문서를 PDF 로 변환한다. 원본이 이미 PDF 면 변환 없이 그대로 사용한다.
     * 성공 시 반환되는 경로는 임시 디렉터리(.tmp) 안의 파일이므로 호출측이 보관 또는 삭제해야 한다.
     */
    public Result convert(Path source, String ext) {
        if ("pdf".equalsIgnoreCase(ext)) {
            return new Result(source, false, null);
        }
        if (!available) {
            return Result.fail("PDF 변환기를 사용할 수 없습니다.");
        }

        boolean acquired;
        try {
            acquired = gate.tryAcquire(timeoutSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.fail("변환이 중단되었습니다.");
        }
        if (!acquired) {
            return Result.fail("변환 대기 시간이 초과되었습니다.");
        }

        Path work = null;
        try {
            work = Files.createTempDirectory(tmpRoot, "soffice-");
            return runSoffice(source, work);
        } catch (IOException e) {
            log.warn("PDF 변환 실패: {}", source, e);
            return Result.fail("PDF 변환 중 오류가 발생했습니다.");
        } finally {
            gate.release();
            deleteRecursively(work);
        }
    }

    private Result runSoffice(Path source, Path work) throws IOException {
        Path profile = work.resolve("profile");
        Path logFile = work.resolve("soffice.log");

        ProcessBuilder pb = new ProcessBuilder(
                sofficeBin,
                "--headless", "--invisible", "--nologo", "--nodefault",
                "--norestore", "--nolockcheck", "--nofirststartwizard",
                // 프로필을 매 실행마다 분리해야 "another instance is running" 충돌이 없다
                "-env:UserInstallation=file://" + profile.toAbsolutePath(),
                "--convert-to", "pdf",
                "--outdir", work.toString(),
                source.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        // 파이프를 읽지 않으면 출력이 많을 때 프로세스가 멈춘다 → 파일로 흘려보낸다
        pb.redirectOutput(logFile.toFile());
        // soffice 는 쓰기 가능한 HOME 이 필요하다 (systemd 로 띄우면 없을 수 있음)
        pb.environment().put("HOME", work.toAbsolutePath().toString());

        Process process = pb.start();
        try {
            if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                return Result.fail("변환 시간이 초과되었습니다(" + timeoutSec + "초).");
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return Result.fail("변환이 중단되었습니다.");
        }

        // exit code 가 0 이어도 산출물이 없는 경우가 있어 파일 존재를 반드시 확인한다
        Path out = work.resolve(baseName(source) + ".pdf");
        if (process.exitValue() != 0 || !Files.exists(out) || Files.size(out) == 0) {
            log.warn("soffice 변환 실패 exit={} source={} log={}",
                    process.exitValue(), source.getFileName(), tail(logFile));
            return Result.fail("PDF 변환에 실패했습니다.");
        }

        Path moved = Files.move(out, tmpRoot.resolve(UUID.randomUUID().toString().replace("-", "") + ".pdf"));
        return new Result(moved, true, null);
    }

    private String detectBinary() {
        if (configuredBin != null && !configuredBin.isBlank()) {
            return configuredBin.trim();
        }
        String env = System.getenv("SOFFICE_BIN");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return BIN_CANDIDATES.stream()
                .filter(p -> Files.isExecutable(Paths.get(p)))
                .findFirst()
                .orElse(null);
    }

    /** soffice --version 을 짧은 타임아웃으로 1회 실행해 실제 동작 여부를 확인한다. */
    private boolean probe(String bin) {
        try {
            Process p = new ProcessBuilder(bin, "--version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!p.waitFor(15, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String baseName(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String tail(Path logFile) {
        try {
            if (!Files.exists(logFile)) {
                return "";
            }
            String text = Files.readString(logFile);
            return text.length() > 500 ? text.substring(text.length() - 500) : text;
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }

    /** 변환 작업 디렉터리(프로필 포함) 정리. */
    public static void deleteRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 정리 실패는 무시 - 배치가 다시 청소한다
                }
            });
        } catch (IOException e) {
            log.warn("임시 디렉터리 정리 실패: {}", dir);
        }
    }
}
