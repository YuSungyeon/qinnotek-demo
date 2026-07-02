package com.qinnotek.photo.dto.admin;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.CompanyStatus;

/**
 * 관리자 기업 목록 항목. 종합 상태를 색상과 함께 제공.
 */
public record CompanySummaryResponse(
        Long id,
        String name,
        String phoneNumber,
        String status,
        String statusLabel,
        String statusColor
) {
    public static CompanySummaryResponse of(Company company, CompanyStatus status) {
        return new CompanySummaryResponse(
                company.getId(),
                company.getName(),
                company.getPhoneNumber(),
                status.name(),
                status.getLabel(),
                status.getColor()
        );
    }
}
