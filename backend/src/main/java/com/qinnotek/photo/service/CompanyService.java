package com.qinnotek.photo.service;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.CompanyManager;
import com.qinnotek.photo.domain.CompanyStatus;
import com.qinnotek.photo.domain.Manager;
import com.qinnotek.photo.domain.PhotoRequirement;
import com.qinnotek.photo.domain.SubmissionItem;
import com.qinnotek.photo.dto.admin.CompanyDetailResponse;
import com.qinnotek.photo.dto.admin.CompanySummaryResponse;
import com.qinnotek.photo.exception.BusinessException;
import com.qinnotek.photo.repository.CompanyManagerRepository;
import com.qinnotek.photo.repository.CompanyRepository;
import com.qinnotek.photo.repository.ManagerRepository;
import com.qinnotek.photo.repository.PhotoRequirementRepository;
import com.qinnotek.photo.repository.SubmissionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 관리자 기업 관리 - 검색, 전화번호 등록/수정, 요구 사진 지정.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final PhotoRequirementRepository requirementRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final ManagerRepository managerRepository;
    private final CompanyManagerRepository companyManagerRepository;
    private final FileStorageService fileStorageService;

    public List<CompanySummaryResponse> search(String keyword) {
        List<Company> companies;
        if (keyword == null || keyword.isBlank()) {
            companies = companyRepository.findAllByOrderByNameAsc();
        } else {
            companies = companyRepository
                    .findByNameContainingOrPhoneNumberContainingOrderByNameAsc(keyword.trim(), keyword.trim());
        }
        return companies.stream()
                .map(c -> CompanySummaryResponse.of(c, computeStatus(c.getId())))
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
     * 요구 사진 지정 - 선택된 요구사항 목록으로 제출 항목을 동기화한다.
     * 새로 선택된 항목은 추가(PENDING), 해제된 항목은 삭제(사진 파일도 삭제).
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
                PhotoRequirement req = requirementRepository.findById(reqId)
                        .orElseThrow(() -> BusinessException.notFound("요구 사진을 찾을 수 없습니다: " + reqId));
                submissionItemRepository.save(new SubmissionItem(company, req));
            }
        }
        return getDetail(id);
    }

    private CompanyStatus computeStatus(Long companyId) {
        return CompanyStatus.from(submissionItemRepository.findByCompanyId(companyId));
    }
}
