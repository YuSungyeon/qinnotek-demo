package com.qinnotek.submission.dto.admin;

import com.qinnotek.submission.domain.SubmissionEvent;

import java.time.LocalDateTime;

/**
 * 처리 이력 한 줄 (관리자 화면).
 *
 * @param type    SUBMITTED / APPROVED / RETURNED / NOTICE_SENT / NOTICE_FAILED / LINK_ISSUED / LINK_REVOKED / LINK_SMS_SENT
 * @param round   제출 차수 (SUBMITTED). 2 이상이면 재제출
 * @param fileUrl 그 시점의 제출 파일 (반환한 파일도 보관되어 열린다)
 */
public record SubmissionEventResponse(
        Long id,
        String type,
        Long itemId,
        String itemName,
        Integer round,
        String reason,
        String fileUrl,
        String pdfUrl,
        String contentKind,
        String recipient,
        String detail,
        LocalDateTime createdAt
) {
    public static SubmissionEventResponse from(SubmissionEvent e) {
        return new SubmissionEventResponse(
                e.getId(),
                e.getType().name(),
                e.getItemId(),
                e.getItemName(),
                e.getSubmitRound(),
                e.getReason(),
                e.getStoredFileName() == null ? null : "/api/files/" + e.getStoredFileName(),
                e.getPdfFileName() == null ? null : "/api/files/" + e.getPdfFileName(),
                e.getContentKind() == null ? null : e.getContentKind().name(),
                e.getRecipient(),
                e.getDetail(),
                e.getCreatedAt()
        );
    }
}
