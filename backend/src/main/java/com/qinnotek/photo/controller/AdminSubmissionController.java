package com.qinnotek.photo.controller;

import com.qinnotek.photo.dto.admin.CompanySubmissionResponse;
import com.qinnotek.photo.dto.admin.ReturnRequest;
import com.qinnotek.photo.dto.admin.SubmissionItemResponse;
import com.qinnotek.photo.service.AdminSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자-검수", description = "관리자 제출 사진 조회 및 검수 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSubmissionController {

    private final AdminSubmissionService submissionService;

    @Operation(summary = "기업 제출 사진 조회", description = "기업명, 제출일시, 사진 목록")
    @GetMapping("/companies/{companyId}/submissions")
    public CompanySubmissionResponse getSubmissions(@PathVariable Long companyId) {
        return submissionService.getCompanySubmissions(companyId);
    }

    @Operation(summary = "통과", description = "해당 사진을 승인")
    @PostMapping("/submissions/{itemId}/approve")
    public SubmissionItemResponse approve(@PathVariable Long itemId) {
        return submissionService.approve(itemId);
    }

    @Operation(summary = "반환", description = "기존 사진 삭제 후 반환 상태/사유 저장. 반환 사유 필수")
    @PostMapping("/submissions/{itemId}/return")
    public SubmissionItemResponse markReturned(@PathVariable Long itemId,
                                               @Valid @RequestBody ReturnRequest request) {
        return submissionService.markReturned(itemId, request.reason());
    }
}
