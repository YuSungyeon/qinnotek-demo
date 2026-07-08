package com.qinnotek.photo.controller;

import com.qinnotek.photo.dto.admin.CompanySubmissionResponse;
import com.qinnotek.photo.dto.admin.ReturnRequest;
import com.qinnotek.photo.dto.admin.SubmissionItemResponse;
import com.qinnotek.photo.service.AdminSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    @Operation(summary = "완료 사진 ZIP 다운로드", description = "통과된 사진들을 기업명_사진명.zip 하나로 묶어 다운로드")
    @GetMapping("/companies/{companyId}/submissions/approved-zip")
    public ResponseEntity<Resource> downloadApprovedZip(@PathVariable Long companyId) {
        AdminSubmissionService.ZipResult zip = submissionService.zipApprovedPhotos(companyId);
        String encoded = URLEncoder.encode(zip.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(new ByteArrayResource(zip.data()));
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

    public record NoteRequest(String note) {
    }

    @Operation(summary = "기업별 추가 설명 저장", description = "해당 항목에 관리자 추가 설명 저장(빈 값이면 삭제). 고객 화면에 표시됨")
    @PutMapping("/submissions/{itemId}/note")
    public SubmissionItemResponse updateNote(@PathVariable Long itemId,
                                             @RequestBody NoteRequest request) {
        return submissionService.updateNote(itemId, request.note());
    }
}
