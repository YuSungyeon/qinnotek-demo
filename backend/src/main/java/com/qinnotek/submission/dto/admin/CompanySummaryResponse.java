package com.qinnotek.submission.dto.admin;

import com.qinnotek.submission.domain.Company;
import com.qinnotek.submission.domain.CompanyStatus;
import com.qinnotek.submission.domain.SubmissionItem;
import com.qinnotek.submission.domain.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 관리자 기업 목록 한 줄.
 *
 * @param totalItems       지정된 요청 자료 수
 * @param submittedItems   고객이 제출을 마친 수 (검수 대기 + 통과). 목록의 진행 막대
 * @param hasLink          제출 링크가 발급되어 있는지
 * @param linkOpenedAt     고객이 링크를 마지막으로 연 시각 (안 열었으면 null) - "아직 열지 않음" 파악용
 * @param latestSubmittedAt 가장 최근 제출 시각
 */
public record CompanySummaryResponse(
        Long id,
        String name,
        String phoneNumber,
        String status,
        String statusLabel,
        int totalItems,
        int submittedItems,
        boolean hasLink,
        LocalDateTime linkOpenedAt,
        LocalDateTime latestSubmittedAt
) {
    public static CompanySummaryResponse of(Company company, List<SubmissionItem> items) {
        CompanyStatus status = CompanyStatus.from(items);
        int submitted = (int) items.stream()
                .filter(i -> i.getStatus() == SubmissionStatus.SUBMITTED || i.getStatus() == SubmissionStatus.APPROVED)
                .count();
        LocalDateTime latest = items.stream()
                .map(SubmissionItem::getUploadedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new CompanySummaryResponse(
                company.getId(),
                company.getName(),
                company.getPhoneNumber(),
                status.name(),
                status.getLabel(),
                items.size(),
                submitted,
                company.getAccessToken() != null,
                company.getLinkOpenedAt(),
                latest
        );
    }
}
