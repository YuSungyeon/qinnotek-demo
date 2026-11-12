package com.qinnotek.photo.dto.admin;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.CompanyStatus;

import java.time.LocalDateTime;
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
        List<Long> assignedManagerIds,
        Link link
) {
    /** 제출 링크 (미발급이면 null) */
    public record Link(String token, LocalDateTime issuedAt, LocalDateTime expiresAt,
                       LocalDateTime openedAt, boolean expired) {
    }

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
                assignedManagerIds,
                company.getAccessToken() == null ? null : new Link(
                        company.getAccessToken(),
                        company.getLinkIssuedAt(),
                        company.getLinkExpiresAt(),
                        company.getLinkOpenedAt(),
                        company.isLinkExpired(LocalDateTime.now()))
        );
    }
}
