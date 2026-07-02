package com.qinnotek.photo.controller;

import com.qinnotek.photo.dto.admin.AdminSettingDto;
import com.qinnotek.photo.service.AdminSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자-설정", description = "관리자 전역 설정(알림 문자 수신 번호)")
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingController {

    private final AdminSettingService settingService;

    @Operation(summary = "설정 조회", description = "관리자 전화번호와 SMS 연동 여부")
    @GetMapping
    public AdminSettingDto.Response get() {
        return settingService.get();
    }

    @Operation(summary = "관리자 전화번호 저장", description = "고객 제출 시 알림 문자를 받을 번호")
    @PutMapping("/admin-phone")
    public AdminSettingDto.Response updateAdminPhone(@RequestBody AdminSettingDto.UpdatePhone request) {
        return settingService.updateAdminPhone(request.adminPhoneNumber());
    }
}
