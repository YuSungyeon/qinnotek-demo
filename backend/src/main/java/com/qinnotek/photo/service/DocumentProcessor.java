package com.qinnotek.photo.service;

import com.qinnotek.photo.domain.StagedDocument;
import com.qinnotek.photo.repository.StagedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.Locale;

/**
 * 스테이징된 문서를 PDF 로 변환하고 분류용 텍스트를 뽑는다.
 *
 * <p>StagedDocumentService 와 별도 빈으로 둔다 - 같은 빈 안에서 @Async 메서드를 호출하면
 * 프록시를 거치지 않아 동기 실행되기 때문이다.
 *
 * <p>분류는 여기서 하지 않는다. 문서가 순차로 도착하므로 마지막 문서까지 모인 뒤
 * 조회 시점에 한꺼번에 1:1 배정해야 결과가 안정적이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessor {

    private final StagedDocumentRepository stagedDocumentRepository;
    private final FileStorageService fileStorageService;
    private final PdfConversionService pdfConversionService;
    private final DocumentTextExtractor textExtractor;

    @Async("documentExecutor")
    @Transactional
    public void process(Long stagedId) {
        StagedDocument staged = stagedDocumentRepository.findById(stagedId).orElse(null);
        if (staged == null) {
            log.warn("스테이징 문서를 찾을 수 없습니다: {}", stagedId);
            return;
        }
        try {
            processInternal(staged);
        } catch (RuntimeException e) {
            // 워커가 죽어도 문서는 PENDING 으로 남지 않도록 반드시 상태를 확정한다
            log.error("문서 처리 실패: {}", stagedId, e);
            staged.markFailed("문서를 처리하지 못했습니다.");
        }
    }

    private void processInternal(StagedDocument staged) {
        Path source = Path.of(staged.getStoragePath());
        String ext = extensionOf(staged.getStoredFileName());

        var conversion = pdfConversionService.convert(source, ext);
        if (!conversion.success()) {
            log.info("PDF 변환 실패({}) - 원본만 보관합니다: {}", conversion.message(), staged.getOriginalFileName());
            staged.markFailed(conversion.message());
            return;
        }

        // 원본이 이미 PDF 면 사본을 따로 만들지 않고 같은 파일을 가리킨다
        String pdfFileName = conversion.converted()
                ? fileStorageService.storeFromPath(conversion.pdfPath(), pdfName(staged), "pdf").storedFileName()
                : staged.getStoredFileName();

        var extracted = textExtractor.extract(fileStorageService.pathOf(pdfFileName));
        staged.markProcessed(pdfFileName, extracted.text(), extracted.status());
    }

    private static String pdfName(StagedDocument staged) {
        String original = staged.getOriginalFileName();
        if (original == null || original.isBlank()) {
            return "document.pdf";
        }
        int dot = original.lastIndexOf('.');
        return (dot > 0 ? original.substring(0, dot) : original) + ".pdf";
    }

    private static String extensionOf(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }
}
