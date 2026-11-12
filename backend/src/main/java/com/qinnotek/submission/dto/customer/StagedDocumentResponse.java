package com.qinnotek.submission.dto.customer;

import com.qinnotek.submission.domain.Confidence;
import com.qinnotek.submission.domain.StagedDocument;
import com.qinnotek.submission.service.DocumentClassifier;

/**
 * 스테이징된 문서 1건. 변환 상태와 자동 배정 제안을 함께 담는다.
 */
public record StagedDocumentResponse(
        Long stagedId,
        String originalFileName,
        long sizeBytes,
        /** PENDING / READY / FAILED */
        String conversionStatus,
        String failureMessage,
        String pdfUrl,
        /** 자동 배정된 항목. 확신이 낮아 배정을 포기하면 null */
        Long suggestedItemId,
        /** HIGH / LOW / NONE */
        String confidence,
        double score
) {
    public static StagedDocumentResponse from(StagedDocument staged, DocumentClassifier.Assignment assignment) {
        String pdfUrl = staged.getPdfFileName() != null ? "/api/files/" + staged.getPdfFileName() : null;
        Confidence confidence = assignment == null ? null : assignment.confidence();
        return new StagedDocumentResponse(
                staged.getId(),
                staged.getOriginalFileName(),
                staged.getSizeBytes(),
                staged.getConversionStatus().name(),
                staged.getFailureMessage(),
                pdfUrl,
                assignment == null ? null : assignment.itemId(),
                confidence == null ? null : confidence.name(),
                assignment == null ? 0 : assignment.score()
        );
    }
}
