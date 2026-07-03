package com.qinnotek.photo.service;

import com.qinnotek.photo.config.AppProperties;
import com.qinnotek.photo.domain.AdminSetting;
import com.qinnotek.photo.dto.admin.AdminSettingDto;
import com.qinnotek.photo.repository.AdminSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 전역 설정 관리 (알림 문자 수신 번호).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSettingService {

    private final AdminSettingRepository repository;
    private final AppProperties appProperties;

    @Transactional
    public AdminSettingDto.Response get() {
        AdminSetting setting = repository.findTopByOrderByIdAsc()
                .orElseGet(() -> repository.save(new AdminSetting()));
        return toResponse(setting);
    }

    @Transactional
    public AdminSettingDto.Response updateAdminPhone(String phoneNumber) {
        AdminSetting setting = repository.findTopByOrderByIdAsc().orElseGet(AdminSetting::new);
        setting.changeAdminPhoneNumber(phoneNumber);
        repository.save(setting);
        return toResponse(setting);
    }

    @Transactional
    public AdminSettingDto.Response updateTheme(String designId, String themeId, String primaryColor) {
        AdminSetting setting = repository.findTopByOrderByIdAsc().orElseGet(AdminSetting::new);
        setting.changeTheme(designId, themeId, primaryColor);
        repository.save(setting);
        return toResponse(setting);
    }

    /** 공개 테마 조회 (설정 없으면 기본값) */
    public AdminSettingDto.Theme getTheme() {
        return repository.findTopByOrderByIdAsc()
                .map(s -> new AdminSettingDto.Theme(
                        s.getDesignId() == null ? "base" : s.getDesignId(),
                        s.getThemeId() == null ? "blue" : s.getThemeId(),
                        s.getPrimaryColor()))
                .orElse(new AdminSettingDto.Theme("base", "blue", null));
    }

    /** 알림 발송에 사용할 관리자 번호 (없으면 null) */
    public String getAdminPhoneNumber() {
        return repository.findTopByOrderByIdAsc()
                .map(AdminSetting::getAdminPhoneNumber)
                .orElse(null);
    }

    private AdminSettingDto.Response toResponse(AdminSetting setting) {
        var sms = appProperties.getSms();
        boolean configured = sms.isEnabled()
                && sms.getApiKey() != null && !sms.getApiKey().isBlank()
                && sms.getApiSecret() != null && !sms.getApiSecret().isBlank();
        return new AdminSettingDto.Response(
                setting.getAdminPhoneNumber(),
                configured,
                setting.getDesignId() == null ? "base" : setting.getDesignId(),
                setting.getThemeId() == null ? "blue" : setting.getThemeId(),
                setting.getPrimaryColor());
    }
}
