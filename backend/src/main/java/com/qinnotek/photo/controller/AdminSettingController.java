package com.qinnotek.photo.controller;

import com.qinnotek.photo.dto.admin.AdminSettingDto;
import com.qinnotek.photo.service.AdminSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자-설정", description = "관리자 전역 설정(디자인/테마, SMS 연동)")
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingController {

    private final AdminSettingService settingService;

    @Operation(summary = "설정 조회", description = "디자인/테마, SMS 연동 여부")
    @GetMapping
    public AdminSettingDto.Response get() {
        return settingService.get();
    }

    @Operation(summary = "테마/색상 저장", description = "프리셋 테마 id 또는 커스텀 기본색(hex) 저장. 전 화면에 반영")
    @PutMapping("/theme")
    public AdminSettingDto.Response updateTheme(@RequestBody AdminSettingDto.UpdateTheme request) {
        return settingService.updateTheme(request.designId(), request.themeId(), request.primaryColor());
    }

    @Operation(summary = "문자 발송 on/off", description = "관리자 알림 문자 발송 토글")
    @PutMapping("/sms")
    public AdminSettingDto.Response updateSms(@RequestBody AdminSettingDto.UpdateSms request) {
        return settingService.updateSmsEnabled(request.enabled());
    }
}
