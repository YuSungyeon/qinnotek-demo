package com.qinnotek.photo.controller;

import com.qinnotek.photo.dto.customer.CustomerPhotoItemResponse;
import com.qinnotek.photo.dto.customer.CustomerStatusResponse;
import com.qinnotek.photo.dto.customer.LookupRequest;
import com.qinnotek.photo.dto.customer.SubmitRequest;
import com.qinnotek.photo.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "고객", description = "고객 사진 제출 API")
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "전화번호 조회", description = "전화번호로 기업을 조회하고 현재 제출 상태와 표시할 사진 항목을 반환")
    @PostMapping("/lookup")
    public CustomerStatusResponse lookup(@Valid @RequestBody LookupRequest request) {
        return customerService.lookup(request.phoneNumber());
    }

    @Operation(summary = "사진 업로드", description = "개별 항목의 사진을 업로드/교체 (항목당 1장, 이미지 파일만)")
    @PostMapping(value = "/items/{itemId}/photo", consumes = "multipart/form-data")
    public CustomerPhotoItemResponse uploadPhoto(@PathVariable Long itemId,
                                                 @RequestParam("file") MultipartFile file) {
        return customerService.uploadPhoto(itemId, file);
    }

    @Operation(summary = "최종 제출", description = "모든 대상 사진 업로드 완료 시 검수 대기 상태로 전환하고 관리자에게 알림 발송")
    @PostMapping("/submit")
    public CustomerStatusResponse submit(@Valid @RequestBody SubmitRequest request) {
        return customerService.submit(request.companyId());
    }
}
