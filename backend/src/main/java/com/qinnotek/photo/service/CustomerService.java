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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 고객 흐름 - 제출 링크 열기, 사진 업로드, 제출.
 * 모든 고객 요청은 링크 토큰으로 기업을 확정한 뒤, 그 기업의 항목에만 손댈 수 있다.
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
     * 링크 토큰으로 기업을 확정한다. 없는 토큰은 404, 기간이 지난 토큰은 410.
     */
    public Company resolve(String token) {
        Company company = (token == null || token.isBlank()) ? null
                : companyRepository.findByAccessToken(token.trim()).orElse(null);
        if (company == null) {
            throw BusinessException.notFound("유효하지 않은 링크입니다.");
        }
        if (company.isLinkExpired(LocalDateTime.now())) {
            throw BusinessException.gone("기간이 지난 링크입니다.");
        }
        return company;
    }

    /**
     * 링크를 열었을 때 - 현재 제출 상태를 반환하고 마지막 접속 시각을 남긴다.
     */
    @Transactional
    public CustomerStatusResponse open(String token) {
        Company company = resolve(token);
        company.markLinkOpened(LocalDateTime.now());
        List<SubmissionItem> items = submissionItemRepository.findByCompanyIdWithRequirement(company.getId());
        if (items.isEmpty()) {
            throw BusinessException.notFound("아직 요청된 자료가 없습니다.");
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
                    "반환된 항목을 다시 제출해주세요.", returned);
        }
        if (anySubmitted) {
            // 검수 대기: 추가 제출 불가
            return new CustomerStatusResponse(company.getId(), company.getName(),
                    CustomerStatusResponse.State.UNDER_REVIEW.name(),
                    "현재 관리자가 제출본을 확인 중입니다.", List.of());
        }
        if (anyPending) {
            // 최초 제출: 모든 필수 항목 표시
            List<CustomerPhotoItemResponse> pending = items.stream()
                    .filter(i -> i.getStatus() == SubmissionStatus.PENDING)
                    .map(CustomerPhotoItemResponse::from)
                    .toList();
            return new CustomerStatusResponse(company.getId(), company.getName(),
                    CustomerStatusResponse.State.INITIAL.name(),
                    "필요한 항목을 모두 올린 후 제출해주세요.", pending);
        }
        // 모두 통과
        return new CustomerStatusResponse(company.getId(), company.getName(),
                CustomerStatusResponse.State.COMPLETED.name(),
                "모든 항목이 통과되었습니다. 감사합니다.", List.of());
    }

    /**
     * 개별 항목 사진 업로드/교체. 항상 최신 1장만 유지.
     */
    @Transactional
    public CustomerPhotoItemResponse uploadPhoto(String token, Long itemId, MultipartFile file) {
        Company company = resolve(token);
        SubmissionItem item = submissionItemRepository.findById(itemId)
                .filter(i -> i.getCompany().getId().equals(company.getId()))
                .orElseThrow(() -> BusinessException.notFound("제출 항목을 찾을 수 없습니다."));
        if (!item.isActionableByCustomer()) {
            throw BusinessException.badRequest("현재 제출할 수 없는 항목입니다.");
        }
        if (!item.getRequirement().getFileType().acceptsPhoto()) {
            throw BusinessException.badRequest(
                    "'" + item.getRequirement().getName() + "' 항목은 문서 파일만 제출할 수 있습니다.");
        }
        String oldStored = item.getStoredFileName();
        String oldPdf = item.getPdfFileName();
        FileStorageService.StoredFile stored = fileStorageService.store(file);
        item.attachPhoto(stored.storedFileName(), stored.originalFileName(), stored.storagePath());
        // 기존 파일 교체 - 문서였다면 PDF 사본도 함께 지운다(원본이 PDF 면 같은 파일이라 한 번만)
        if (oldStored != null && !oldStored.equals(stored.storedFileName())) {
            fileStorageService.delete(oldStored);
        }
        if (oldPdf != null && !oldPdf.equals(oldStored)) {
            fileStorageService.delete(oldPdf);
        }
        return CustomerPhotoItemResponse.from(item);
    }

    /**
     * 최종 제출. 대상(미제출/반환) 항목이 모두 업로드된 경우에만 검수 대기로 전환.
     */
    @Transactional
    public CustomerStatusResponse submit(String token) {
        Company company = resolve(token);
        List<SubmissionItem> items = submissionItemRepository.findByCompanyIdWithRequirement(company.getId());
        List<SubmissionItem> actionable = items.stream()
                .filter(SubmissionItem::isActionableByCustomer)
                .toList();

        if (actionable.isEmpty()) {
            throw BusinessException.badRequest("제출할 항목이 없습니다.");
        }
        boolean allUploaded = actionable.stream().allMatch(SubmissionItem::hasPhoto);
        if (!allUploaded) {
            throw BusinessException.badRequest("아직 올리지 않은 항목이 있습니다.");
        }

        actionable.forEach(SubmissionItem::submit);
        smsService.notifyManagersOnSubmission(company.getId(), company.getName());

        return buildStatus(company, items);
    }
}
