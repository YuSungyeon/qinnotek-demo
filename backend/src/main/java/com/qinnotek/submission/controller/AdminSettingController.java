package com.qinnotek.submission.controller;

import com.qinnotek.submission.dto.admin.AdminSettingDto;
import com.qinnotek.submission.service.AdminSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자-설정", description = "관리자 전역 설정(제출 알림 문자)")
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingController {

    private final AdminSettingService settingService;

    @Operation(summary = "설정 조회", description = "제출 알림 문자 on/off, 서버 발송 준비 여부")
    @GetMapping
    public AdminSettingDto.Response get() {
        return settingService.get();
    }

    @Operation(summary = "문자 발송 on/off", description = "관리자 알림 문자 발송 토글")
    @PutMapping("/sms")
    public AdminSettingDto.Response updateSms(@RequestBody AdminSettingDto.UpdateSms request) {
        return settingService.updateSmsEnabled(request.enabled());
    }
}
