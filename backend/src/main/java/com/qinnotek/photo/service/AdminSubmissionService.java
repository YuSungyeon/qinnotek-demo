package com.qinnotek.photo.service;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.CompanyStatus;
import com.qinnotek.photo.domain.SubmissionItem;
import com.qinnotek.photo.dto.admin.CompanySubmissionResponse;
import com.qinnotek.photo.dto.admin.SubmissionItemResponse;
import com.qinnotek.photo.exception.BusinessException;
import com.qinnotek.photo.repository.CompanyRepository;
import com.qinnotek.photo.repository.SubmissionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 관리자 제출 사진 조회 및 검수(통과/반환).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSubmissionService {

    private final CompanyRepository companyRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final FileStorageService fileStorageService;

    public CompanySubmissionResponse getCompanySubmissions(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> BusinessException.notFound("기업을 찾을 수 없습니다."));
        List<SubmissionItem> items = submissionItemRepository.findByCompanyIdWithRequirement(companyId);

        List<SubmissionItemResponse> itemResponses = items.stream()
                .map(SubmissionItemResponse::from)
                .toList();

        LocalDateTime latest = items.stream()
                .map(SubmissionItem::getUploadedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        CompanyStatus status = CompanyStatus.from(items);
        return new CompanySubmissionResponse(
                company.getId(), company.getName(), company.getPhoneNumber(),
                status.name(), status.getLabel(), status.getColor(),
                latest, itemResponses);
    }

    @Transactional
    public SubmissionItemResponse approve(Long itemId) {
        SubmissionItem item = getItemOrThrow(itemId);
        item.approve();
        return SubmissionItemResponse.from(item);
    }

    /**
     * 반환 - 기존 사진 삭제 후 반환 상태/사유 저장. 고객은 재촬영 필요.
     */
    @Transactional
    public SubmissionItemResponse markReturned(Long itemId, String reason) {
        SubmissionItem item = getItemOrThrow(itemId);
        String stored = item.getStoredFileName();
        item.markReturned(reason);
        fileStorageService.delete(stored);
        return SubmissionItemResponse.from(item);
    }

    private SubmissionItem getItemOrThrow(Long itemId) {
        return submissionItemRepository.findById(itemId)
                .orElseThrow(() -> BusinessException.notFound("제출 항목을 찾을 수 없습니다."));
    }
}
