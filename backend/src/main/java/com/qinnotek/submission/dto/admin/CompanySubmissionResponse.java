package com.qinnotek.submission.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 특정 기업의 제출 자료 조회 결과.
 */
public record CompanySubmissionResponse(
        Long companyId,
        String companyName,
        String phoneNumber,
        String status,
        String statusLabel,
        LocalDateTime latestSubmittedAt,
        List<SubmissionItemResponse> items
) {
}
