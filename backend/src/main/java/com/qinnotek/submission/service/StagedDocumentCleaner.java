package com.qinnotek.submission.service;

import com.qinnotek.submission.domain.StagedDocument;
import com.qinnotek.submission.domain.StagedState;
import com.qinnotek.submission.repository.StagedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 확정되지 않은 스테이징 문서와 변환 임시 파일을 청소한다.
 *
 * <p>문서는 분류를 위해 "전송하기" 전에 미리 올라가므로, 이탈한 세션이 남긴 파일을 주기적으로 지워야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StagedDocumentCleaner {

    private static final Duration STAGED_TTL = Duration.ofHours(24);
    private static final Duration TMP_TTL = Duration.ofHours(1);

    private final StagedDocumentRepository stagedDocumentRepository;
    private final StagedDocumentService stagedDocumentService;
    private final FileStorageService fileStorageService;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgeStale() {
        purgeStagedDocuments();
        purgeTempFiles();
    }

    private void purgeStagedDocuments() {
        List<StagedDocument> stale =
                stagedDocumentRepository.findByCreatedAtBefore(LocalDateTime.now().minus(STAGED_TTL));
        if (stale.isEmpty()) {
            return;
        }
        for (StagedDocument staged : stale) {
            // CONSUMED 는 파일 소유권이 SubmissionItem 으로 넘어갔으므로 행만 지운다
            if (staged.getState() == StagedState.STAGED) {
                stagedDocumentService.deleteFiles(staged);
            }
        }
        stagedDocumentRepository.deleteAll(stale);
        log.info("스테이징 문서 정리: {}건", stale.size());
    }

    /** soffice 가 강제 종료되면 작업 디렉터리가 남는다. */
    private void purgeTempFiles() {
        Path tmpRoot = fileStorageService.getTmpRoot();
        if (!Files.isDirectory(tmpRoot)) {
            return;
        }
        Instant cutoff = Instant.now().minus(TMP_TTL);
        try (var entries = Files.list(tmpRoot)) {
            entries.filter(p -> isOlderThan(p, cutoff)).forEach(p -> {
                if (Files.isDirectory(p)) {
                    PdfConversionService.deleteRecursively(p);
                } else {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // 다음 주기에 다시 시도한다
                    }
                }
            });
        } catch (IOException e) {
            log.warn("임시 디렉터리 정리 실패: {}", tmpRoot, e);
        }
    }

    private static boolean isOlderThan(Path path, Instant cutoff) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
        } catch (IOException e) {
            return false;
        }
    }
}
