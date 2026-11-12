package com.qinnotek.submission.controller;

import com.qinnotek.submission.dto.customer.AttachDocumentRequest;
import com.qinnotek.submission.dto.customer.CustomerItemResponse;
import com.qinnotek.submission.dto.customer.CustomerStatusResponse;
import com.qinnotek.submission.dto.customer.StagedDocumentResponse;
import com.qinnotek.submission.service.CustomerService;
import com.qinnotek.submission.service.StagedDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 고객 API. 모든 경로는 관리자가 발급한 링크 토큰 아래에 있고,
 * 서버가 토큰으로 기업을 확정한 뒤 그 기업의 항목·문서만 다룬다.
 */
@Tag(name = "고객", description = "제출 링크로 접속한 고객의 사진·문서 제출 API")
@RestController
@RequestMapping("/api/customer/links/{token}")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final StagedDocumentService stagedDocumentService;

    @Operation(summary = "링크 열기", description = "기업명, 현재 제출 상태, 표시할 항목을 반환. 없는 링크 404, 기간 지난 링크 410")
    @GetMapping
    public CustomerStatusResponse open(@PathVariable String token) {
        return customerService.open(token);
    }

    @Operation(summary = "사진 업로드", description = "개별 항목의 사진을 업로드/교체 (항목당 1장, 이미지 파일만)")
    @PostMapping(value = "/items/{itemId}/photo", consumes = "multipart/form-data")
    public CustomerItemResponse uploadPhoto(@PathVariable String token,
                                                 @PathVariable Long itemId,
                                                 @RequestParam("file") MultipartFile file) {
        return customerService.uploadPhoto(token, itemId, file);
    }

    @Operation(summary = "최종 제출", description = "모든 대상 항목 업로드 완료 시 검수 대기 상태로 전환하고 관리자에게 알림 발송")
    @PostMapping("/submit")
    public CustomerStatusResponse submit(@PathVariable String token) {
        return customerService.submit(token);
    }

    @Operation(summary = "문서 올리기",
            description = "문서를 임시 보관하고 PDF 변환/텍스트 추출을 예약한다. 변환은 비동기라 즉시 반환되며, 진행 상태는 목록 API 로 확인한다.")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = "/documents", consumes = "multipart/form-data")
    public StagedDocumentResponse stageDocument(@PathVariable String token,
                                                @RequestParam("file") MultipartFile file) {
        return stagedDocumentService.stage(customerService.resolve(token).getId(), file);
    }

    @Operation(summary = "올린 문서 목록",
            description = "아직 항목에 확정되지 않은 문서와 변환 상태, 자동 배정 제안을 반환")
    @GetMapping("/documents")
    public List<StagedDocumentResponse> listStagedDocuments(@PathVariable String token) {
        return stagedDocumentService.list(customerService.resolve(token).getId());
    }

    @Operation(summary = "올린 문서 취소")
    @DeleteMapping("/documents/{stagedId}")
    public void discardStagedDocument(@PathVariable String token, @PathVariable Long stagedId) {
        stagedDocumentService.discard(customerService.resolve(token).getId(), stagedId);
    }

    @Operation(summary = "문서를 항목에 확정", description = "올려둔 문서를 해당 제출 항목의 제출본으로 지정")
    @PostMapping("/items/{itemId}/attach-document")
    public CustomerItemResponse attachDocument(@PathVariable String token,
                                                    @PathVariable Long itemId,
                                                    @Valid @RequestBody AttachDocumentRequest request) {
        return stagedDocumentService.attach(customerService.resolve(token).getId(), itemId, request.stagedId());
    }
}
