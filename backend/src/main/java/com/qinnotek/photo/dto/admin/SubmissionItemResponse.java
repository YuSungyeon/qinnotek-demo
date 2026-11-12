package com.qinnotek.photo.dto.admin;

import com.qinnotek.photo.domain.SubmissionItem;

import java.time.LocalDateTime;

/**
 * 관리자 제출 항목 조회. 표시명은 기업명_항목명 형태.
 */
public record SubmissionItemResponse(
        Long itemId,
        String requirementName,
        String displayName,
        String status,
        String statusLabel,
        String rejectReason,
        String adminNote,
        String photoUrl,
        String originalFileName,
        LocalDateTime uploadedAt,
        /** PHOTO / DOCUMENT / ANY - 항목이 받는 파일 유형 */
        String fileType,
        /** PHOTO / DOCUMENT - 실제 올라온 파일 종류. 미제출이면 null */
        String contentKind,
        String pdfUrl,
        /** READY / FAILED - 문서일 때만 의미 있음 */
        String pdfStatus,
        String originalDownloadUrl,
        String pdfDownloadUrl
) {
    public static SubmissionItemResponse from(SubmissionItem item) {
        String companyName = item.getCompany().getName();
        String requirementName = item.getRequirement().getName();
        String photoUrl = item.hasPhoto() ? "/api/files/" + item.getStoredFileName() : null;
        String pdfUrl = item.hasPdfCopy() ? "/api/files/" + item.getPdfFileName() : null;
        return new SubmissionItemResponse(
                item.getId(),
                requirementName,
                companyName + "_" + requirementName,
                item.getStatus().name(),
                item.getStatus().getLabel(),
                item.getRejectReason(),
                item.getAdminNote(),
                photoUrl,
                item.getOriginalFileName(),
                item.getUploadedAt(),
                item.getRequirement().getFileType().name(),
                item.hasPhoto() ? item.getContentKind().name() : null,
                pdfUrl,
                item.getPdfStatus() == null ? null : item.getPdfStatus().name(),
                item.hasPhoto() ? "/api/admin/submissions/" + item.getId() + "/original" : null,
                item.hasPdfCopy() ? "/api/admin/submissions/" + item.getId() + "/pdf" : null
        );
    }
}
