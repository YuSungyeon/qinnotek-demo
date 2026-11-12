package com.qinnotek.submission.controller;

import com.qinnotek.submission.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@Tag(name = "파일", description = "업로드 파일 제공")
@RestController
@RequiredArgsConstructor
public class FileController {

    /** MediaTypeFactory 가 모르는 확장자 보정. */
    private static final Map<String, MediaType> EXTRA_TYPES = Map.of(
            "hwp", MediaType.parseMediaType("application/x-hwp"),
            "hwpx", MediaType.parseMediaType("application/hwp+zip")
    );

    private final FileStorageService fileStorageService;

    @Operation(summary = "파일 조회",
            description = "UUID 저장 파일명으로 파일을 반환. 이미지/PDF 만 브라우저에 인라인으로 열리고 나머지는 다운로드된다.")
    @GetMapping("/api/files/{fileName}")
    public ResponseEntity<Resource> serve(@PathVariable String fileName,
                                          @RequestParam(defaultValue = "false") boolean download) {
        Resource resource = fileStorageService.loadAsResource(fileName);
        MediaType mediaType = resolveMediaType(fileName);

        boolean inlineable = "image".equals(mediaType.getType())
                || MediaType.APPLICATION_PDF.equalsTypeAndSubtype(mediaType);
        String disposition = (download || !inlineable) ? "attachment" : "inline";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, private")
                // 업로드된 파일이 다른 타입으로 해석되어 실행되는 것을 막는다
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(resource);
    }

    private MediaType resolveMediaType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            MediaType extra = EXTRA_TYPES.get(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
            if (extra != null) {
                return extra;
            }
        }
        return MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}
