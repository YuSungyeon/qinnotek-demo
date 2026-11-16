package com.qinnotek.submission.service;

import com.qinnotek.submission.config.AppProperties;
import com.qinnotek.submission.domain.Company;
import com.qinnotek.submission.domain.CompanyManager;
import com.qinnotek.submission.domain.CompanyStatus;
import com.qinnotek.submission.domain.Manager;
import com.qinnotek.submission.domain.Requirement;
import com.qinnotek.submission.domain.SubmissionItem;
import com.qinnotek.submission.dto.admin.CompanyDetailResponse;
import com.qinnotek.submission.dto.admin.CompanySummaryResponse;
import com.qinnotek.submission.exception.BusinessException;
import com.qinnotek.submission.repository.CompanyManagerRepository;
import com.qinnotek.submission.repository.CompanyRepository;
import com.qinnotek.submission.repository.ManagerRepository;
import com.qinnotek.submission.repository.RequirementRepository;
import com.qinnotek.submission.repository.SubmissionItemRepository;
import com.qinnotek.submission.util.LinkTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 관리자 기업 관리 - 검색, 제출 링크 발급, 요청 자료 지정.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final RequirementRepository requirementRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final ManagerRepository managerRepository;
    private final CompanyManagerRepository companyManagerRepository;
    private final FileStorageService fileStorageService;
    private final SmsService smsService;
    private final AppProperties appProperties;

    public List<CompanySummaryResponse> search(String keyword) {
        List<Company> companies;
        if (keyword == null || keyword.isBlank()) {
            companies = companyRepository.findAllByOrderByNameAsc();
        } else {
            companies = companyRepository
                    .findByNameContainingOrPhoneNumberContainingOrderByNameAsc(keyword.trim(), keyword.trim());
        }
        return companies.stream()
                .map(c -> CompanySummaryResponse.of(c, submissionItemRepository.findByCompanyId(c.getId())))
                .toList();
    }

    public Company getOrThrow(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("기업을 찾을 수 없습니다."));
    }

    public CompanyDetailResponse getDetail(Long id) {
        Company company = getOrThrow(id);
        List<Long> assignedReq = submissionItemRepository.findByCompanyIdWithRequirement(id).stream()
                .map(i -> i.getRequirement().getId())
                .toList();
        List<Long> assignedMgr = companyManagerRepository.findByCompanyIdWithManager(id).stream()
                .map(cm -> cm.getManager().getId())
                .toList();
        return CompanyDetailResponse.of(company, computeStatus(id), assignedReq, assignedMgr);
    }

    @Transactional
    public CompanyDetailResponse create(String name) {
        String trimmed = name.trim();
        if (companyRepository.existsByName(trimmed)) {
            throw BusinessException.conflict("이미 등록된 기업명입니다: " + trimmed);
        }
        Company saved = companyRepository.save(new Company(trimmed));
        return CompanyDetailResponse.of(saved, CompanyStatus.NONE, List.of(), List.of());
    }

    @Transactional
    public CompanyDetailResponse updateName(Long id, String name) {
        Company company = getOrThrow(id);
        String trimmed = name.trim();
        if (!company.getName().equals(trimmed) && companyRepository.existsByName(trimmed)) {
            throw BusinessException.conflict("이미 등록된 기업명입니다: " + trimmed);
        }
        company.changeName(trimmed);
        return getDetail(id);
    }

    /** 기업 삭제 - 제출 항목·파일·담당자 지정도 함께 삭제 */
    @Transactional
    public void delete(Long id) {
        Company company = getOrThrow(id);
        List<SubmissionItem> items = submissionItemRepository.findByCompanyId(id);
        items.forEach(i -> fileStorageService.delete(i.getStoredFileName()));
        submissionItemRepository.deleteAll(items);
        companyManagerRepository.deleteAll(companyManagerRepository.findByCompanyId(id));
        companyRepository.delete(company);
    }

    /** 알림 담당자 지정 - 선택된 담당자 목록으로 연결을 동기화한다. */
    @Transactional
    public CompanyDetailResponse assignManagers(Long id, List<Long> managerIds) {
        Company company = getOrThrow(id);
        Set<Long> targetIds = new LinkedHashSet<>(managerIds == null ? List.of() : managerIds);

        List<CompanyManager> existing = companyManagerRepository.findByCompanyIdWithManager(id);
        Set<Long> existingIds = existing.stream()
                .map(cm -> cm.getManager().getId())
                .collect(Collectors.toSet());

        // 해제된 담당자 연결 삭제
        for (CompanyManager cm : existing) {
            if (!targetIds.contains(cm.getManager().getId())) {
                companyManagerRepository.delete(cm);
            }
        }
        // 새로 선택된 담당자 연결 추가
        for (Long managerId : targetIds) {
            if (!existingIds.contains(managerId)) {
                Manager manager = managerRepository.findById(managerId)
                        .orElseThrow(() -> BusinessException.notFound("담당자를 찾을 수 없습니다: " + managerId));
                companyManagerRepository.save(new CompanyManager(company, manager));
            }
        }
        return getDetail(id);
    }

    /** 제출 링크 발급/재발급. 재발급하면 이전 링크는 즉시 무효가 된다. */
    @Transactional
    public CompanyDetailResponse issueLink(Long id, Integer validDays) {
        Company company = getOrThrow(id);
        if (validDays != null && (validDays < 1 || validDays > 365)) {
            throw BusinessException.badRequest("유효 기간은 1~365일이어야 합니다.");
        }
        String token;
        do {
            token = LinkTokens.generate();
        } while (companyRepository.findByAccessToken(token).isPresent());
        LocalDateTime expiresAt = validDays == null ? null : LocalDateTime.now().plusDays(validDays);
        company.issueLink(token, expiresAt);
        return getDetail(id);
    }

    /** 제출 링크 끄기 */
    @Transactional
    public CompanyDetailResponse revokeLink(Long id) {
        getOrThrow(id).revokeLink();
        return getDetail(id);
    }

    /**
     * 현재 제출 링크를 문자로 보낸다. 주소는 서버가 설정값(app.public-url)으로 만든다 -
     * 요청에서 받은 주소를 보내면 회사 발신번호로 아무 사이트 주소나 보낼 수 있게 된다.
     */
    public void sendLinkSms(Long id, String to) {
        Company company = getOrThrow(id);
        String token = company.getAccessToken();
        if (token == null || company.isLinkExpired(LocalDateTime.now())) {
            throw BusinessException.badRequest("사용 중인 링크가 없습니다. 링크를 먼저 만들어주세요.");
        }
        String url = appProperties.getPublicUrl().replaceAll("/+$", "") + "/s/" + token;
        String digits = to == null ? "" : to.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 11) {
            throw BusinessException.badRequest("받는 번호를 확인해주세요.");
        }
        smsService.sendLink(digits, company.getName(), url);
    }

    @Transactional
    public CompanyDetailResponse updatePhone(Long id, String phoneNumber) {
        Company company = getOrThrow(id);
        String normalized = (phoneNumber == null || phoneNumber.isBlank()) ? null : phoneNumber.trim();
        if (normalized != null) {
            companyRepository.findByPhoneNumber(normalized)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw BusinessException.conflict("이미 다른 기업에 등록된 전화번호입니다.");
                    });
        }
        company.changePhoneNumber(normalized);
        return getDetail(id);
    }

    /**
     * 요청 자료 지정 - 선택된 요청 자료 목록으로 제출 항목을 동기화한다.
     * 새로 선택된 항목은 추가(PENDING), 해제된 항목은 삭제(올린 파일도 삭제).
     */
    @Transactional
    public CompanyDetailResponse assignRequirements(Long id, List<Long> requirementIds) {
        Company company = getOrThrow(id);
        Set<Long> targetIds = new LinkedHashSet<>(
                requirementIds == null ? List.of() : requirementIds);

        List<SubmissionItem> existing = submissionItemRepository.findByCompanyIdWithRequirement(id);
        Map<Long, SubmissionItem> existingByReq = existing.stream()
                .collect(Collectors.toMap(i -> i.getRequirement().getId(), i -> i));

        // 삭제 대상: 기존에 있으나 이번에 선택되지 않은 것
        for (SubmissionItem item : existing) {
            if (!targetIds.contains(item.getRequirement().getId())) {
                fileStorageService.delete(item.getStoredFileName());
                submissionItemRepository.delete(item);
            }
        }
        // 추가 대상: 새로 선택된 것
        for (Long reqId : targetIds) {
            if (!existingByReq.containsKey(reqId)) {
                Requirement req = requirementRepository.findById(reqId)
                        .orElseThrow(() -> BusinessException.notFound("요청 자료를 찾을 수 없습니다: " + reqId));
                submissionItemRepository.save(new SubmissionItem(company, req));
            }
        }
        return getDetail(id);
    }

    private CompanyStatus computeStatus(Long companyId) {
        return CompanyStatus.from(submissionItemRepository.findByCompanyId(companyId));
    }
}
