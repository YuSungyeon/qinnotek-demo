package com.qinnotek.photo.dto.admin;

import com.qinnotek.photo.domain.SubmissionItem;

import java.time.LocalDateTime;

/**
 * 관리자 제출 사진 조회 항목. 표시명은 기업명_사진명 형태.
 */
public record SubmissionItemResponse(
        Long itemId,
        String requirementName,
        String displayName,
        String status,
        String statusLabel,
        String rejectReason,
        String photoUrl,
        String originalFileName,
        LocalDateTime uploadedAt
) {
    public static SubmissionItemResponse from(SubmissionItem item) {
        String companyName = item.getCompany().getName();
        String requirementName = item.getRequirement().getName();
        String photoUrl = item.hasPhoto() ? "/api/files/" + item.getStoredFileName() : null;
        return new SubmissionItemResponse(
                item.getId(),
                requirementName,
                companyName + "_" + requirementName,
                item.getStatus().name(),
                item.getStatus().getLabel(),
                item.getRejectReason(),
                photoUrl,
                item.getOriginalFileName(),
                item.getUploadedAt()
        );
    }
}
