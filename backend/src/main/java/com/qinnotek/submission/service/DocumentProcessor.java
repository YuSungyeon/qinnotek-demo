package com.qinnotek.submission.service;

import com.qinnotek.submission.domain.StagedDocument;
import com.qinnotek.submission.domain.StagedState;
import com.qinnotek.submission.domain.TextStatus;
import com.qinnotek.submission.repository.StagedDocumentRepository;
import com.qinnotek.submission.repository.SubmissionItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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
 *
 * <p>고객은 변환이 끝나기 전에도 전송할 수 있다. 그래서 변환(최대 수십 초)은 트랜잭션 밖에서 하고,
 * 결과 기록만 행을 잠근 짧은 트랜잭션에서 한다. 그 사이 문서가 항목에 확정됐으면 항목에도 PDF 사본을 이어 붙인다.
 * (예전처럼 변환 내내 트랜잭션을 잡으면, 그 사이의 확정(CONSUMED)을 낡은 STAGED 로 덮어써
 * 다음 날 청소 작업이 제출된 문서를 지웠다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessor {

    private final StagedDocumentRepository stagedDocumentRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final FileStorageService fileStorageService;
    private final PdfConversionService pdfConversionService;
    private final DocumentTextExtractor textExtractor;
    private final TransactionTemplate transactionTemplate;

    @Async("documentExecutor")
    public void process(Long stagedId) {
        processNow(stagedId);
    }

    /** 같은 작업을 호출 스레드에서 바로 실행한다 (테스트용 진입점이기도 하다). */
    public void processNow(Long stagedId) {
        StagedDocument snapshot = stagedDocumentRepository.findById(stagedId).orElse(null);
        if (snapshot == null) {
            log.warn("스테이징 문서를 찾을 수 없습니다: {}", stagedId);
            return;
        }
        Outcome outcome;
        try {
            outcome = convert(snapshot);
        } catch (RuntimeException e) {
            // 워커가 죽어도 문서는 PENDING 으로 남지 않도록 반드시 상태를 확정한다
            log.error("문서 처리 실패: {}", stagedId, e);
            outcome = Outcome.failed("문서를 처리하지 못했습니다.");
        }
        Outcome result = outcome;
        transactionTemplate.executeWithoutResult(status -> record(stagedId, snapshot.getStoredFileName(), result));
    }

    private Outcome convert(StagedDocument staged) {
        Path source = Path.of(staged.getStoragePath());
        String ext = extensionOf(staged.getStoredFileName());

        var conversion = pdfConversionService.convert(source, ext);
        if (!conversion.success()) {
            log.info("PDF 변환 실패({}) - 원본만 보관합니다: {}", conversion.message(), staged.getOriginalFileName());
            return Outcome.failed(conversion.message());
        }

        // 원본이 이미 PDF 면 사본을 따로 만들지 않고 같은 파일을 가리킨다
        String pdfFileName = conversion.converted()
                ? fileStorageService.storeFromPath(conversion.pdfPath(), pdfName(staged), "pdf").storedFileName()
                : staged.getStoredFileName();

        var extracted = textExtractor.extract(fileStorageService.pathOf(pdfFileName));
        return new Outcome(pdfFileName, extracted.text(), extracted.status(), null);
    }

    /** 변환 결과 기록. 행을 잠그고 최신 상태로 다시 읽어 고객의 확정·취소와 순서를 맞춘다. */
    private void record(Long stagedId, String originalStored, Outcome outcome) {
        StagedDocument staged = stagedDocumentRepository.findByIdForUpdate(stagedId).orElse(null);
        if (staged == null) {
            // 변환 중에 고객이 취소했다 - 새로 만든 사본만 치운다
            deleteCopy(outcome, originalStored);
            return;
        }
        if (outcome.success()) {
            staged.markProcessed(outcome.pdfFileName(), outcome.text(), outcome.textStatus());
        } else {
            staged.markFailed(outcome.failure());
        }
        if (staged.getState() != StagedState.CONSUMED) {
            return;
        }
        // 변환 중에 이미 항목에 확정됐다. 항목이 아직 이 문서를 들고 있으면 결과를 이어 붙이고,
        // 그새 다른 파일로 교체·반환됐으면 사본은 주인이 없으니 지운다.
        submissionItemRepository.findByStoredFileName(originalStored).ifPresentOrElse(
                item -> item.updatePdf(staged.getPdfFileName(), staged.getConversionStatus()),
                () -> deleteCopy(outcome, originalStored));
    }

    /** 원본과 별도로 만든 PDF 사본만 지운다 (원본이 PDF 면 같은 파일이므로 건드리지 않는다). */
    private void deleteCopy(Outcome outcome, String originalStored) {
        if (outcome.pdfFileName() != null && !outcome.pdfFileName().equals(originalStored)) {
            fileStorageService.delete(outcome.pdfFileName());
        }
    }

    /** 변환 결과. failure 가 null 이면 성공. */
    private record Outcome(String pdfFileName, String text, TextStatus textStatus, String failure) {
        static Outcome failed(String message) {
            return new Outcome(null, null, null, message);
        }

        boolean success() {
            return failure == null;
        }
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
