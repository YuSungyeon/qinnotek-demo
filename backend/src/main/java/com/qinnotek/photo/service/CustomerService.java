package com.qinnotek.photo.service;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.SubmissionItem;
import com.qinnotek.photo.domain.SubmissionStatus;
import com.qinnotek.photo.dto.customer.CustomerPhotoItemResponse;
import com.qinnotek.photo.dto.customer.CustomerStatusResponse;
import com.qinnotek.photo.exception.BusinessException;
import com.qinnotek.photo.repository.CompanyRepository;
import com.qinnotek.photo.repository.SubmissionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 고객 흐름 - 전화번호 조회, 사진 업로드, 제출.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CompanyRepository companyRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final FileStorageService fileStorageService;
    private final SmsService smsService;

    /**
     * 전화번호로 기업을 조회하고 현재 제출 상태를 반환한다.
     */
    public CustomerStatusResponse lookup(String phoneNumber) {
        String normalized = phoneNumber == null ? "" : phoneNumber.trim();
        Company company = companyRepository.findByPhoneNumber(normalized)
                .orElseThrow(() -> BusinessException.notFound("등록되지 않은 전화번호입니다."));

        List<SubmissionItem> items = submissionItemRepository.findByCompanyIdWithRequirement(company.getId());
        if (items.isEmpty()) {
            throw BusinessException.notFound("등록되지 않은 전화번호입니다.");
        }
        return buildStatus(company, items);
    }

    private CustomerStatusResponse buildStatus(Company company, List<SubmissionItem> items) {
        boolean anyReturned = items.stream().anyMatch(i -> i.getStatus() == SubmissionStatus.RETURNED);
        boolean anySubmitted = items.stream().anyMatch(i -> i.getStatus() == SubmissionStatus.SUBMITTED);
        boolean anyPending = items.stream().anyMatch(i -> i.getStatus() == SubmissionStatus.PENDING);

        if (anyReturned) {
            // 반환: 반환된 항목만 표시
            List<CustomerPhotoItemResponse> returned = items.stream()
                    .filter(i -> i.getStatus() == SubmissionStatus.RETURNED)
                    .map(CustomerPhotoItemResponse::from)
                    .toList();
            return new CustomerStatusResponse(company.getId(), company.getName(),
                    CustomerStatusResponse.State.RETURNED.name(),
                    "반환된 사진을 다시 촬영하여 제출해주세요.", returned);
        }
        if (anySubmitted) {
            // 검수 대기: 추가 제출 불가
            return new CustomerStatusResponse(company.getId(), company.getName(),
                    CustomerStatusResponse.State.UNDER_REVIEW.name(),
                    "현재 관리자가 사진을 확인 중입니다.", List.of());
        }
        if (anyPending) {
            // 최초 제출: 모든 필수 항목 표시
            List<CustomerPhotoItemResponse> pending = items.stream()
                    .filter(i -> i.getStatus() == SubmissionStatus.PENDING)
                    .map(CustomerPhotoItemResponse::from)
                    .toList();
            return new CustomerStatusResponse(company.getId(), company.getName(),
                    CustomerStatusResponse.State.INITIAL.name(),
                    "필요한 사진을 모두 업로드한 후 제출해주세요.", pending);
        }
        // 모두 통과
        return new CustomerStatusResponse(company.getId(), company.getName(),
                CustomerStatusResponse.State.COMPLETED.name(),
                "모든 사진이 통과되었습니다. 감사합니다.", List.of());
    }

    /**
     * 개별 항목 사진 업로드/교체. 항상 최신 1장만 유지.
     */
    @Transactional
    public CustomerPhotoItemResponse uploadPhoto(Long itemId, MultipartFile file) {
        SubmissionItem item = submissionItemRepository.findById(itemId)
                .orElseThrow(() -> BusinessException.notFound("제출 항목을 찾을 수 없습니다."));
        if (!item.isActionableByCustomer()) {
            throw BusinessException.badRequest("현재 제출할 수 없는 항목입니다.");
        }
        String oldStored = item.getStoredFileName();
        FileStorageService.StoredFile stored = fileStorageService.store(file);
        item.attachPhoto(stored.storedFileName(), stored.originalFileName(), stored.storagePath());
        // 기존 파일 교체
        if (oldStored != null && !oldStored.equals(stored.storedFileName())) {
            fileStorageService.delete(oldStored);
        }
        return CustomerPhotoItemResponse.from(item);
    }

    /**
     * 최종 제출. 대상(미제출/반환) 항목이 모두 업로드된 경우에만 검수 대기로 전환.
     */
    @Transactional
    public CustomerStatusResponse submit(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> BusinessException.notFound("기업을 찾을 수 없습니다."));

        List<SubmissionItem> items = submissionItemRepository.findByCompanyIdWithRequirement(companyId);
        List<SubmissionItem> actionable = items.stream()
                .filter(SubmissionItem::isActionableByCustomer)
                .toList();

        if (actionable.isEmpty()) {
            throw BusinessException.badRequest("제출할 사진이 없습니다.");
        }
        boolean allUploaded = actionable.stream().allMatch(SubmissionItem::hasPhoto);
        if (!allUploaded) {
            throw BusinessException.badRequest("아직 업로드되지 않은 사진이 있습니다.");
        }

        actionable.forEach(SubmissionItem::submit);
        smsService.notifyAdminOnSubmission(company.getName());

        return buildStatus(company, items);
    }
}
