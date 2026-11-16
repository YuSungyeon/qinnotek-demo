package com.qinnotek.submission.controller;

import com.qinnotek.submission.dto.admin.CompanyDetailResponse;
import com.qinnotek.submission.dto.admin.CompanyRequests;
import com.qinnotek.submission.dto.admin.CompanySummaryResponse;
import com.qinnotek.submission.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자-기업", description = "관리자 기업 관리 API")
@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyService companyService;

    @Operation(summary = "기업 목록/검색", description = "기업명 또는 전화번호로 검색. 종합 상태 포함")
    @GetMapping
    public List<CompanySummaryResponse> search(@RequestParam(required = false) String keyword) {
        return companyService.search(keyword);
    }

    @Operation(summary = "기업 상세", description = "전화번호, 지정된 요청 자료 ID 목록 포함")
    @GetMapping("/{id}")
    public CompanyDetailResponse detail(@PathVariable Long id) {
        return companyService.getDetail(id);
    }

    @Operation(summary = "기업 등록")
    @PostMapping
    public CompanyDetailResponse create(@Valid @RequestBody CompanyRequests.CreateCompany request) {
        return companyService.create(request.name());
    }

    @Operation(summary = "기업명 수정")
    @PutMapping("/{id}/name")
    public CompanyDetailResponse updateName(@PathVariable Long id,
                                            @Valid @RequestBody CompanyRequests.UpdateName request) {
        return companyService.updateName(id, request.name());
    }

    @Operation(summary = "기업 삭제", description = "제출 항목과 업로드 파일도 함께 삭제")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        companyService.delete(id);
    }

    @Operation(summary = "제출 링크 발급/재발급",
            description = "기업당 1개. 재발급하면 이전 링크는 즉시 무효. validDays 가 없으면 기한 없음")
    @PostMapping("/{id}/link")
    public CompanyDetailResponse issueLink(@PathVariable Long id,
                                           @RequestBody(required = false) CompanyRequests.IssueLink request) {
        return companyService.issueLink(id, request == null ? null : request.validDays());
    }

    @Operation(summary = "제출 링크 끄기")
    @DeleteMapping("/{id}/link")
    public CompanyDetailResponse revokeLink(@PathVariable Long id) {
        return companyService.revokeLink(id);
    }

    @Operation(summary = "제출 링크 문자 발송")
    @PostMapping("/{id}/link/sms")
    public void sendLinkSms(@PathVariable Long id, @Valid @RequestBody CompanyRequests.SendLink request) {
        companyService.sendLinkSms(id, request.to());
    }

    @Operation(summary = "전화번호 등록/수정", description = "링크 문자 발송 시 기본 수신 번호")
    @PutMapping("/{id}/phone")
    public CompanyDetailResponse updatePhone(@PathVariable Long id,
                                             @RequestBody CompanyRequests.UpdatePhone request) {
        return companyService.updatePhone(id, request.phoneNumber());
    }

    @Operation(summary = "요청 자료 지정", description = "요청 자료 다중 선택으로 기업의 제출 목록 구성")
    @PutMapping("/{id}/requirements")
    public CompanyDetailResponse assignRequirements(@PathVariable Long id,
                                                    @RequestBody CompanyRequests.AssignRequirements request) {
        return companyService.assignRequirements(id, request.requirementIds());
    }

    @Operation(summary = "알림 담당자 지정", description = "제출 시 문자를 받을 담당자 다중 선택")
    @PutMapping("/{id}/managers")
    public CompanyDetailResponse assignManagers(@PathVariable Long id,
                                                @RequestBody com.qinnotek.submission.dto.admin.ManagerDto.Assign request) {
        return companyService.assignManagers(id, request.managerIds());
    }
}
