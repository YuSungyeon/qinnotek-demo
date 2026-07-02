package com.qinnotek.photo.dto.customer;

import com.qinnotek.photo.domain.SubmissionItem;

/**
 * 고객에게 보여줄 개별 사진 항목.
 */
public record CustomerPhotoItemResponse(
        Long itemId,
        String name,
        String description,
        String exampleImageUrl,
        String status,
        String rejectReason,
        boolean hasPhoto,
        String photoUrl
) {
    public static CustomerPhotoItemResponse from(SubmissionItem item) {
        var req = item.getRequirement();
        String exampleUrl = req.getExampleImageFileName() != null
                ? "/api/files/" + req.getExampleImageFileName() : null;
        String photoUrl = item.hasPhoto() ? "/api/files/" + item.getStoredFileName() : null;
        return new CustomerPhotoItemResponse(
                item.getId(),
                req.getName(),
                req.getDescription(),
                exampleUrl,
                item.getStatus().name(),
                item.getRejectReason(),
                item.hasPhoto(),
                photoUrl
        );
    }
}
