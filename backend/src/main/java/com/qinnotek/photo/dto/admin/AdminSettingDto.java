package com.qinnotek.photo.dto.admin;

/**
 * 관리자 설정 요청/응답 DTO.
 */
public class AdminSettingDto {

    /**
     * @param smsConfigured Solapi Key/Secret 및 활성화가 모두 설정되어 발송 가능한 상태인지
     * @param designId      기업 디자인 팩 id
     * @param themeId       테마 프리셋 id
     * @param primaryColor  커스텀 기본색(hex), 없으면 null
     */
    public record Response(
            boolean smsConfigured,
            String designId,
            String themeId,
            String primaryColor
    ) {
    }

    public record UpdateTheme(String designId, String themeId, String primaryColor) {
    }

    /** 공개 테마 조회용 (고객 화면 포함) */
    public record Theme(String designId, String themeId, String primaryColor) {
    }
}
