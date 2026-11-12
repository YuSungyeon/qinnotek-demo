package com.qinnotek.submission.dto.customer;

import com.qinnotek.submission.domain.SubmissionItem;

/**
 * 고객에게 보여줄 개별 제출 항목.
 */
public record CustomerItemResponse(
        Long itemId,
        String name,
        String description,
        String adminNote,
        String classificationHint,
        String exampleImageUrl,
        String status,
        String rejectReason,
        boolean hasFile,
        String fileUrl,
        /** PHOTO / DOCUMENT / ANY - 이 항목이 받는 파일 유형 */
        String fileType,
        /** 실제 올라온 파일 종류 (PHOTO / DOCUMENT) */
        String contentKind,
        String originalFileName,
        String pdfUrl,
        /** READY / FAILED - 문서일 때만 의미 있음 */
        String pdfStatus
) {
    public static CustomerItemResponse from(SubmissionItem item) {
        var req = item.getRequirement();
        String exampleUrl = req.getExampleImageFileName() != null
                ? "/api/files/" + req.getExampleImageFileName() : null;
        String fileUrl = item.hasFile() ? "/api/files/" + item.getStoredFileName() : null;
        String pdfUrl = item.hasPdfCopy() ? "/api/files/" + item.getPdfFileName() : null;
        return new CustomerItemResponse(
                item.getId(),
                req.getName(),
                req.getDescription(),
                item.getAdminNote(),
                req.getClassificationHint(),
                exampleUrl,
                item.getStatus().name(),
                item.getRejectReason(),
                item.hasFile(),
                fileUrl,
                req.getFileType().name(),
                item.hasFile() ? item.getContentKind().name() : null,
                item.getOriginalFileName(),
                pdfUrl,
                item.getPdfStatus() == null ? null : item.getPdfStatus().name()
        );
    }
}
