package com.qinnotek.submission.service;

import com.qinnotek.submission.domain.Company;
import com.qinnotek.submission.domain.SubmissionItem;
import com.qinnotek.submission.domain.SubmissionStatus;
import com.qinnotek.submission.dto.customer.CustomerItemResponse;
import com.qinnotek.submission.dto.customer.CustomerStatusResponse;
import com.qinnotek.submission.exception.BusinessException;
import com.qinnotek.submission.repository.CompanyRepository;
import com.qinnotek.submission.repository.SubmissionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
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
    private final SubmissionHistoryService historyService;

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

    /**
     * 고객 화면 상태. 일부만 먼저 낼 수 있으므로, 아직 낼 항목(미제출·반환)이 있으면
     * 검수 중인 항목이 있어도 화면을 잠그지 않고 그 항목들을 보여준다.
     */
    private CustomerStatusResponse buildStatus(Company company, List<SubmissionItem> items) {
        // 반환된 항목을 먼저, 이어서 아직 안 낸 항목 (각각 원래 순서 유지)
        List<CustomerItemResponse> toUpload = items.stream()
                .filter(SubmissionItem::isActionableByCustomer)
                .sorted(Comparator.comparing((SubmissionItem i) -> i.getStatus() != SubmissionStatus.RETURNED))
                .map(CustomerItemResponse::from)
                .toList();
        List<String> reviewing = items.stream()
                .filter(i -> i.getStatus() == SubmissionStatus.SUBMITTED)
                .map(i -> i.getRequirement().getName())
                .toList();
        int approved = (int) items.stream().filter(i -> i.getStatus() == SubmissionStatus.APPROVED).count();

        CustomerStatusResponse.State state;
        String message;
        if (!toUpload.isEmpty()) {
            boolean onlyReturned = items.stream().noneMatch(i -> i.getStatus() == SubmissionStatus.PENDING);
            state = onlyReturned ? CustomerStatusResponse.State.RETURNED : CustomerStatusResponse.State.INITIAL;
            message = onlyReturned ? "반환된 항목을 다시 제출해주세요." : "필요한 항목을 올려 제출해주세요. 일부만 먼저 보낼 수도 있어요.";
        } else if (!reviewing.isEmpty()) {
            state = CustomerStatusResponse.State.UNDER_REVIEW;
            message = "현재 관리자가 제출본을 확인 중입니다.";
        } else {
            state = CustomerStatusResponse.State.COMPLETED;
            message = "모든 항목이 통과되었습니다. 감사합니다.";
        }
        return new CustomerStatusResponse(company.getId(), company.getName(),
                state.name(), message, toUpload, reviewing, approved);
    }

    /**
     * 개별 항목 사진 업로드/교체. 항상 최신 1장만 유지.
     */
    @Transactional
    public CustomerItemResponse uploadPhoto(String token, Long itemId, MultipartFile file) {
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
        return CustomerItemResponse.from(item);
    }

    /**
     * 제출. 고객이 고른 항목만 검수 대기로 넘긴다 - 일부만 먼저 내고 남은 항목은 같은 링크로 이어서 낸다.
     *
     * @param itemIds 낼 항목. null 이면 파일을 올려 둔 항목을 모두 낸다.
     *                고른 항목은 모두 파일이 있어야 하고, 이 기업의 낼 수 있는 항목이 아닌 id 는 무시한다.
     *                (화면에 보이지 않는, 예전에 올리다 만 파일이 고객 모르게 같이 제출되지 않도록 목록을 받는다)
     */
    @Transactional
    public CustomerStatusResponse submit(String token, List<Long> itemIds) {
        Company company = resolve(token);
        List<SubmissionItem> items = submissionItemRepository.findByCompanyIdWithRequirement(company.getId());
        List<SubmissionItem> actionable = items.stream()
                .filter(SubmissionItem::isActionableByCustomer)
                .toList();

        List<SubmissionItem> targets;
        if (itemIds == null) {
            targets = actionable.stream().filter(SubmissionItem::hasFile).toList();
        } else {
            targets = actionable.stream().filter(i -> itemIds.contains(i.getId())).toList();
            if (targets.stream().anyMatch(i -> !i.hasFile())) {
                throw BusinessException.badRequest("아직 올리지 않은 항목이 있습니다.");
            }
        }
        if (targets.isEmpty()) {
            throw BusinessException.badRequest("제출할 항목이 없습니다.");
        }

        targets.forEach(SubmissionItem::submit);
        historyService.recordSubmitted(targets);
        smsService.notifyManagersOnSubmission(company.getId(), company.getName(), targets.size());

        return buildStatus(company, items);
    }
}
