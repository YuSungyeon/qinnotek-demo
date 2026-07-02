package com.qinnotek.photo.dto.admin;

/**
 * 관리자 설정 요청/응답 DTO.
 */
public class AdminSettingDto {

    /**
     * @param adminPhoneNumber 저장된 관리자 전화번호
     * @param smsConfigured    Solapi Key/Secret 및 활성화가 모두 설정되어 발송 가능한 상태인지
     */
    public record Response(String adminPhoneNumber, boolean smsConfigured) {
    }

    public record UpdatePhone(String adminPhoneNumber) {
    }
}
