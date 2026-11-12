package com.qinnotek.photo.service;

import com.qinnotek.photo.domain.*;
import com.qinnotek.photo.dto.customer.CustomerPhotoItemResponse;
import com.qinnotek.photo.dto.customer.StagedDocumentResponse;
import com.qinnotek.photo.exception.BusinessException;
import com.qinnotek.photo.repository.CompanyRepository;
import com.qinnotek.photo.repository.StagedDocumentRepository;
import com.qinnotek.photo.repository.SubmissionItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 고객이 올린 문서의 스테이징 - 업로드, 변환 상태 조회(자동 배정 제안 포함), 항목 확정.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StagedDocumentService {

    private final CompanyRepository companyRepository;
    private final StagedDocumentRepository stagedDocumentRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final FileStorageService fileStorageService;
    private final DocumentProcessor documentProcessor;
    private final DocumentClassifier documentClassifier;

    /**
     * 문서를 임시 보관하고 변환/추출 작업을 예약한다. 변환은 비동기라 즉시 반환된다.
     */
    @Transactional
    public StagedDocumentResponse stage(Long companyId, MultipartFile file) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> BusinessException.notFound("기업을 찾을 수 없습니다."));

        var stored = fileStorageService.store(file, FileStorageService.FileKind.DOCUMENT);
        StagedDocument staged = stagedDocumentRepository.save(new StagedDocument(
                company, stored.storedFileName(), stored.originalFileName(), stored.storagePath(),
                file.getSize()));

        // 커밋 후에 시작해야 워커가 아직 보이지 않는 행을 조회하지 않는다
        Long stagedId = staged.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                documentProcessor.process(stagedId);
            }
        });
        return StagedDocumentResponse.from(staged, null);
    }

    /**
     * 아직 확정되지 않은 문서 목록과 자동 배정 제안을 반환한다.
     *
     * <p>배정은 조회 시점에 매번 다시 계산한다. 문서가 순차로 도착하므로
     * 개별 처리 시점에 배정하면 1:1 매칭이 성립하지 않는다.
     */
    public List<StagedDocumentResponse> list(Long companyId) {
        List<StagedDocument> staged =
                stagedDocumentRepository.findByCompanyIdAndStateOrderByIdAsc(companyId, StagedState.STAGED);
        if (staged.isEmpty()) {
            return List.of();
        }

        List<DocumentClassifier.Slot> slots = documentSlots(companyId);
        List<DocumentClassifier.Doc> docs = staged.stream()
                .filter(s -> !s.isProcessing())
                .map(s -> new DocumentClassifier.Doc(
                        s.getId(), s.getOriginalFileName(), nullToEmpty(s.getExtractedText())))
                .toList();

        Map<Long, DocumentClassifier.Assignment> byStagedId = new HashMap<>();
        for (var assignment : documentClassifier.assign(docs, slots)) {
            byStagedId.put(assignment.stagedId(), assignment);
        }
        return staged.stream()
                .map(s -> StagedDocumentResponse.from(s, byStagedId.get(s.getId())))
                .toList();
    }

    /** 문서를 제출 항목에 확정한다. */
    @Transactional
    public CustomerPhotoItemResponse attach(Long companyId, Long itemId, Long stagedId) {
        SubmissionItem item = submissionItemRepository.findById(itemId)
                .filter(i -> i.getCompany().getId().equals(companyId))
                .orElseThrow(() -> BusinessException.notFound("제출 항목을 찾을 수 없습니다."));
        if (!item.isActionableByCustomer()) {
            throw BusinessException.badRequest("현재 제출할 수 없는 항목입니다.");
        }
        if (!item.getRequirement().getFileType().acceptsDocument()) {
            throw BusinessException.badRequest("'" + item.getRequirement().getName() + "' 항목은 사진만 제출할 수 있습니다.");
        }

        StagedDocument staged = stagedDocumentRepository.findById(stagedId)
                .orElseThrow(() -> BusinessException.notFound("올린 문서를 찾을 수 없습니다."));
        if (staged.getState() != StagedState.STAGED) {
            throw BusinessException.badRequest("이미 사용된 문서입니다.");
        }
        // 다른 기업의 문서를 붙이지 못하게 한다
        if (!staged.getCompany().getId().equals(item.getCompany().getId())) {
            throw BusinessException.badRequest("잘못된 문서입니다.");
        }

        String oldStored = item.getStoredFileName();
        String oldPdf = item.getPdfFileName();

        item.attachDocument(staged.getStoredFileName(), staged.getOriginalFileName(),
                staged.getStoragePath(), staged.getPdfFileName(), staged.getConversionStatus());
        staged.consume();

        deleteReplaced(oldStored, oldPdf, item);
        return CustomerPhotoItemResponse.from(item);
    }

    /** 고객이 올린 문서를 취소한다. */
    @Transactional
    public void discard(Long companyId, Long stagedId) {
        StagedDocument staged = stagedDocumentRepository.findById(stagedId)
                .filter(d -> d.getCompany().getId().equals(companyId))
                .orElse(null);
        if (staged == null) {
            return;
        }
        if (staged.getState() == StagedState.CONSUMED) {
            throw BusinessException.badRequest("이미 제출 항목에 사용된 문서입니다.");
        }
        deleteFiles(staged);
        stagedDocumentRepository.delete(staged);
    }

    /** 문서를 받을 수 있는 항목만 슬롯으로 만든다. 이미 파일이 붙은 항목은 제외. */
    private List<DocumentClassifier.Slot> documentSlots(Long companyId) {
        return submissionItemRepository.findByCompanyIdWithRequirement(companyId).stream()
                .filter(SubmissionItem::isActionableByCustomer)
                .filter(i -> !i.hasPhoto())
                .filter(i -> i.getRequirement().getFileType().acceptsDocument())
                .map(i -> new DocumentClassifier.Slot(
                        i.getId(),
                        i.getRequirement().getName(),
                        i.getRequirement().getDescription(),
                        i.getRequirement().getDocumentKeywords()))
                .toList();
    }

    /** 교체된 이전 파일 삭제. 원본이 PDF 였다면 pdfFileName 이 원본과 같으므로 이중 삭제를 피한다. */
    private void deleteReplaced(String oldStored, String oldPdf, SubmissionItem item) {
        if (oldStored != null && !oldStored.equals(item.getStoredFileName())) {
            fileStorageService.delete(oldStored);
        }
        if (oldPdf != null && !oldPdf.equals(oldStored) && !oldPdf.equals(item.getPdfFileName())) {
            fileStorageService.delete(oldPdf);
        }
    }

    void deleteFiles(StagedDocument staged) {
        fileStorageService.delete(staged.getStoredFileName());
        String pdf = staged.getPdfFileName();
        if (pdf != null && !pdf.equals(staged.getStoredFileName())) {
            fileStorageService.delete(pdf);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
