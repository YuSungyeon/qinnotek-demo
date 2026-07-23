package com.qinnotek.photo.dto.admin;

/**
 * 관리자 설정 요청/응답 DTO.
 */
public class AdminSettingDto {

    /**
     * @param smsEnabled   관리자 문자 발송 토글 on/off
     * @param smsKeyReady  서버에 Solapi 키가 설정되어 실제 발송 가능한지
     * @param designId     기업 디자인 팩 id
     * @param themeId      테마 프리셋 id
     * @param primaryColor 커스텀 기본색(hex), 없으면 null
     */
    public record Response(
            boolean smsEnabled,
            boolean smsKeyReady,
            String designId,
            String themeId,
            String primaryColor
    ) {
    }

    public record UpdateTheme(String designId, String themeId, String primaryColor) {
    }

    public record UpdateSms(boolean enabled) {
    }

    /** 공개 테마 조회용 (고객 화면 포함) */
    public record Theme(String designId, String themeId, String primaryColor) {
    }
}
