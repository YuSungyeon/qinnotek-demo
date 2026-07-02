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
        return new AdminSettingDto.Response(setting.getAdminPhoneNumber(), configured);
    }
}
