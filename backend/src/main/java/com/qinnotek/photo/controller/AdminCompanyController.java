package com.qinnotek.photo.controller;

import com.qinnotek.photo.dto.admin.CompanyDetailResponse;
import com.qinnotek.photo.dto.admin.CompanyRequests;
import com.qinnotek.photo.dto.admin.CompanySummaryResponse;
import com.qinnotek.photo.service.CompanyService;
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

    @Operation(summary = "기업 상세", description = "전화번호, 지정된 요구 사진 ID 목록 포함")
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

    @Operation(summary = "기업 삭제", description = "제출 항목과 업로드 사진도 함께 삭제")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        companyService.delete(id);
    }

    @Operation(summary = "전화번호 등록/수정", description = "기업당 1개, 중복 불가")
    @PutMapping("/{id}/phone")
    public CompanyDetailResponse updatePhone(@PathVariable Long id,
                                             @RequestBody CompanyRequests.UpdatePhone request) {
        return companyService.updatePhone(id, request.phoneNumber());
    }

    @Operation(summary = "필요 사진 지정", description = "요구 사진 다중 선택으로 기업의 제출 목록 구성")
    @PutMapping("/{id}/requirements")
    public CompanyDetailResponse assignRequirements(@PathVariable Long id,
                                                    @RequestBody CompanyRequests.AssignRequirements request) {
        return companyService.assignRequirements(id, request.requirementIds());
    }
}
