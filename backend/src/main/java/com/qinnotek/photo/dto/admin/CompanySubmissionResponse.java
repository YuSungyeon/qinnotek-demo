package com.qinnotek.photo.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 특정 기업의 제출 사진 조회 결과.
 */
public record CompanySubmissionResponse(
        Long companyId,
        String companyName,
        String phoneNumber,
        String status,
        String statusLabel,
        String statusColor,
        LocalDateTime latestSubmittedAt,
        List<SubmissionItemResponse> items
) {
}
