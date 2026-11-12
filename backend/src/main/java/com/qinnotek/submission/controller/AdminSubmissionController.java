package com.qinnotek.submission.controller;

import com.qinnotek.submission.dto.admin.CompanySubmissionResponse;
import com.qinnotek.submission.dto.admin.ReturnRequest;
import com.qinnotek.submission.dto.admin.SubmissionItemResponse;
import com.qinnotek.submission.service.AdminSubmissionService;
import com.qinnotek.submission.service.FileStorageService;
import com.qinnotek.submission.util.HttpFiles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.zip.ZipOutputStream;

@Tag(name = "관리자-검수", description = "관리자 제출 자료 조회 및 검수 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSubmissionController {

    private final AdminSubmissionService submissionService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "기업 제출 자료 조회", description = "기업명, 제출일시, 자료 목록")
    @GetMapping("/companies/{companyId}/submissions")
    public CompanySubmissionResponse getSubmissions(@PathVariable Long companyId) {
        return submissionService.getCompanySubmissions(companyId);
    }

    @Operation(summary = "완료본 ZIP 다운로드",
            description = "통과된 제출본을 하나로 묶어 다운로드. scope=both(기본)/pdf/original. 메모리에 쌓지 않고 스트리밍한다.")
    @GetMapping("/companies/{companyId}/submissions/approved-zip")
    public ResponseEntity<StreamingResponseBody> downloadApprovedZip(
            @PathVariable Long companyId,
            @RequestParam(required = false) String scope) {

        var plan = submissionService.planApprovedZip(companyId, AdminSubmissionService.ZipScope.from(scope));
        StreamingResponseBody body = out -> {
            try (var zos = new ZipOutputStream(out)) {
                for (var entry : plan.entries()) {
                    submissionService.writeZipEntry(zos, entry);
                }
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, HttpFiles.contentDisposition("attachment", plan.fileName()))
                .body(body);
    }

    @Operation(summary = "제출 원본 다운로드", description = "고객이 올린 원본 파일을 원래 파일명으로 다운로드")
    @GetMapping("/submissions/{itemId}/original")
    public ResponseEntity<Resource> downloadOriginal(@PathVariable Long itemId) {
        return download(submissionService.originalDownload(itemId));
    }

    @Operation(summary = "제출 PDF 사본 다운로드", description = "워드/한글 등을 변환한 PDF 사본을 다운로드")
    @GetMapping("/submissions/{itemId}/pdf")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long itemId) {
        return download(submissionService.pdfDownload(itemId));
    }

    private ResponseEntity<Resource> download(AdminSubmissionService.DownloadTarget target) {
        Resource resource = fileStorageService.loadAsResource(target.storedFileName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        HttpFiles.contentDisposition("attachment", target.downloadName()))
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @Operation(summary = "통과", description = "해당 자료를 승인")
    @PostMapping("/submissions/{itemId}/approve")
    public SubmissionItemResponse approve(@PathVariable Long itemId) {
        return submissionService.approve(itemId);
    }

    @Operation(summary = "반환", description = "기존 파일 삭제 후 반환 상태/사유 저장. 반환 사유 필수")
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
