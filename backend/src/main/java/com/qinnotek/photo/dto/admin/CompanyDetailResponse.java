package com.qinnotek.photo.dto.admin;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.CompanyStatus;

import java.util.List;

/**
 * 관리자 기업 상세. 현재 지정된 요구 사진 ID 목록 포함.
 */
public record CompanyDetailResponse(
        Long id,
        String name,
        String phoneNumber,
        String status,
        String statusLabel,
        String statusColor,
        List<Long> assignedRequirementIds
) {
    public static CompanyDetailResponse of(Company company, CompanyStatus status, List<Long> assignedRequirementIds) {
        return new CompanyDetailResponse(
                company.getId(),
                company.getName(),
                company.getPhoneNumber(),
                status.name(),
                status.getLabel(),
                status.getColor(),
                assignedRequirementIds
        );
    }
}
