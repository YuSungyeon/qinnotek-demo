package com.qinnotek.photo.dto.admin;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.CompanyStatus;

import java.util.List;

/**
 * 관리자 기업 상세. 지정된 요구 사진 ID / 알림 담당자 ID 목록 포함.
 */
public record CompanyDetailResponse(
        Long id,
        String name,
        String phoneNumber,
        String status,
        String statusLabel,
        String statusColor,
        List<Long> assignedRequirementIds,
        List<Long> assignedManagerIds
) {
    public static CompanyDetailResponse of(Company company, CompanyStatus status,
                                           List<Long> assignedRequirementIds,
                                           List<Long> assignedManagerIds) {
        return new CompanyDetailResponse(
                company.getId(),
                company.getName(),
                company.getPhoneNumber(),
                status.name(),
                status.getLabel(),
                status.getColor(),
                assignedRequirementIds,
                assignedManagerIds
        );
    }
}
