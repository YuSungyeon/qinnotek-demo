package com.qinnotek.photo.service;

import com.qinnotek.photo.config.AppProperties;
import com.qinnotek.photo.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

/**
 * 로컬 파일시스템 기반 사진 저장. 실제 파일명은 UUID 사용.
 */
@Slf4j
@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(AppProperties appProperties) {
        this.uploadRoot = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(uploadRoot);
            log.info("업로드 디렉터리: {}", uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + uploadRoot, e);
        }
    }

    /** 저장 결과 - UUID 파일명과 저장 경로. */
    public record StoredFile(String storedFileName, String originalFileName, String storagePath) {
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("업로드할 파일이 없습니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw BusinessException.badRequest("이미지 파일만 업로드할 수 있습니다.");
        }

        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename());
        String ext = StringUtils.getFilenameExtension(original);
        String stored = UUID.randomUUID().toString().replace("-", "")
                + (ext != null && !ext.isBlank() ? "." + ext.toLowerCase() : "");

        Path target = uploadRoot.resolve(stored).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw BusinessException.badRequest("잘못된 파일 경로입니다.");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 실패: " + stored, e);
        }
        return new StoredFile(stored, original, target.toString());
    }

    public Resource loadAsResource(String storedFileName) {
        try {
            Path file = uploadRoot.resolve(storedFileName).normalize();
            if (!file.startsWith(uploadRoot)) {
                throw BusinessException.badRequest("잘못된 파일 경로입니다.");
            }
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
}
