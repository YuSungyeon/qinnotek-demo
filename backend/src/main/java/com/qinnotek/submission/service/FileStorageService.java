package com.qinnotek.submission.service;

import com.qinnotek.submission.config.AppProperties;
import com.qinnotek.submission.exception.BusinessException;
import com.qinnotek.submission.util.MagicBytes;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 로컬 파일시스템 기반 파일 저장. 실제 파일명은 UUID 사용.
 */
@Slf4j
@Service
public class FileStorageService {

    /** 업로드 종류. 허용 확장자와 크기 상한이 달라진다. */
    public enum FileKind {
        IMAGE, DOCUMENT
    }

    // svg 는 의도적으로 제외한다 - 같은 오리진에서 인라인 서빙되면 스크립트가 실행된다
    private static final Set<String> IMAGE_EXT =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp");
    private static final Set<String> DOCUMENT_EXT =
            Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "hwp", "hwpx");

    private final Path uploadRoot;
    private final Path tmpRoot;
    private final long maxImageBytes;
    private final long maxDocumentBytes;

    public FileStorageService(AppProperties appProperties) {
        this.uploadRoot = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
        this.tmpRoot = uploadRoot.resolve(".tmp");
        this.maxImageBytes = appProperties.getUpload().getMaxImageBytes().toBytes();
        this.maxDocumentBytes = appProperties.getUpload().getMaxDocumentBytes().toBytes();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(uploadRoot);
            Files.createDirectories(tmpRoot);
            log.info("업로드 디렉터리: {}", uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + uploadRoot, e);
        }
    }

    /** 저장 결과 - UUID 파일명과 저장 경로. */
    public record StoredFile(String storedFileName, String originalFileName, String storagePath) {
    }

    /** 이미지 저장 (기존 호출부 호환). */
    public StoredFile store(MultipartFile file) {
        return store(file, FileKind.IMAGE);
    }

    public StoredFile store(MultipartFile file, FileKind kind) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("업로드할 파일이 없습니다.");
        }

        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? defaultName(kind) : file.getOriginalFilename());
        String ext = extensionOf(original);

        validate(file, kind, ext);

        String stored = randomName(ext);
        Path target = resolveInRoot(stored);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 실패: " + stored, e);
        }
        return new StoredFile(stored, original, target.toString());
    }

    /**
     * 서버가 만든 파일(PDF 사본 등)을 업로드 루트로 편입한다.
     * source 는 이동되므로 호출 후 사용할 수 없다.
     */
    public StoredFile storeFromPath(Path source, String originalFileName, String ext) {
        String stored = randomName(ext);
        Path target = resolveInRoot(stored);
        try {
            // 같은 파일시스템(.tmp 는 업로드 루트 하위)이라 대부분 atomic move 로 끝난다
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("파일 이동 실패: " + stored, e);
        }
        return new StoredFile(stored, originalFileName, target.toString());
    }

    /** 변환 작업용 임시 디렉터리 루트 (업로드 루트와 같은 파일시스템). */
    public Path getTmpRoot() {
        return tmpRoot;
    }

    /** 저장 파일명 → 실제 경로. */
    public Path pathOf(String storedFileName) {
        return resolveInRoot(storedFileName);
    }

    public Resource loadAsResource(String storedFileName) {
        try {
            Path file = resolveInRoot(storedFileName);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw BusinessException.notFound("파일을 찾을 수 없습니다: " + storedFileName);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw BusinessException.notFound("파일을 찾을 수 없습니다: " + storedFileName);
        }
    }

    public void delete(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        try {
            Path file = uploadRoot.resolve(storedFileName).normalize();
            if (file.startsWith(uploadRoot)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {}", storedFileName, e);
        }
    }

    // --- 내부 ---

    private void validate(MultipartFile file, FileKind kind, String ext) {
        Set<String> allowed = kind == FileKind.IMAGE ? IMAGE_EXT : DOCUMENT_EXT;
        if (!allowed.contains(ext)) {
            throw BusinessException.badRequest(kind == FileKind.IMAGE
                    ? "이미지 파일만 업로드할 수 있습니다."
                    : "지원하지 않는 문서 형식입니다. (pdf, doc, docx, xls, xlsx, ppt, pptx, hwp, hwpx)");
        }

        long limit = kind == FileKind.IMAGE ? maxImageBytes : maxDocumentBytes;
        if (file.getSize() > limit) {
            throw BusinessException.badRequest(
                    "파일이 너무 큽니다. (최대 " + (limit / 1024 / 1024) + "MB)");
        }

        if (!MagicBytes.matches(readHead(file), ext)) {
            throw BusinessException.badRequest("파일 내용이 확장자와 일치하지 않습니다.");
        }
    }

    private static byte[] readHead(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(MagicBytes.HEAD_SIZE);
        } catch (IOException e) {
            throw BusinessException.badRequest("파일을 읽을 수 없습니다.");
        }
    }

    /** 업로드 루트를 벗어나지 않는 경로로 해석한다. */
    private Path resolveInRoot(String fileName) {
        Path resolved = uploadRoot.resolve(fileName).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw BusinessException.badRequest("잘못된 파일 경로입니다.");
        }
        return resolved;
    }

    private static String randomName(String ext) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return (ext == null || ext.isBlank()) ? uuid : uuid + "." + ext.toLowerCase(Locale.ROOT);
    }

    private static String extensionOf(String fileName) {
        String ext = StringUtils.getFilenameExtension(fileName);
        return ext == null ? "" : ext.toLowerCase(Locale.ROOT);
    }

    private static String defaultName(FileKind kind) {
        return kind == FileKind.IMAGE ? "photo" : "document";
    }
}
