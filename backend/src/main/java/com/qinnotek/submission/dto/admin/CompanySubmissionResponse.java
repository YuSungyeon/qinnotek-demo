package com.qinnotek.submission.dto.admin;

import com.qinnotek.submission.service.ReturnNoticeService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 특정 기업의 제출 자료 조회 결과.
 *
 * @param returnNotice 검수를 마쳤는데 아직 보내지 못한 반환 안내가 있는지와 그 이유
 */
public record CompanySubmissionResponse(
        Long companyId,
        String companyName,
        String phoneNumber,
        String status,
        String statusLabel,
        LocalDateTime latestSubmittedAt,
        List<SubmissionItemResponse> items,
        ReturnNoticeService.Status returnNotice
) {
}
