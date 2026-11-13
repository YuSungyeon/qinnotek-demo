package com.qinnotek.submission.dto.admin;

/**
 * 관리자 설정 요청/응답 DTO.
 */
public class AdminSettingDto {

    /**
     * @param smsEnabled  제출 알림 문자 발송 on/off (관리자 토글)
     * @param smsKeyReady 서버에 Solapi 키가 설정되어 실제 발송 가능한지
     */
    public record Response(
            boolean smsEnabled,
            boolean smsKeyReady
    ) {
    }

    public record UpdateSms(boolean enabled) {
    }
}
