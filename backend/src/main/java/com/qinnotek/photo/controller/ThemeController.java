package com.qinnotek.photo.controller;

import com.qinnotek.photo.dto.admin.AdminSettingDto;
import com.qinnotek.photo.service.AdminSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 테마 조회 (고객/관리자 화면 공통, 앱 시작 시 로드).
 */
@Tag(name = "테마", description = "현재 적용 테마 조회")
@RestController
@RequiredArgsConstructor
public class ThemeController {

    private final AdminSettingService settingService;

    @Operation(summary = "현재 테마 조회")
    @GetMapping("/api/theme")
    public AdminSettingDto.Theme getTheme() {
        return settingService.getTheme();
    }
}
