package com.qinnotek.photo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 전역 설정(단일 행). 알림 문자를 받을 관리자 전화번호를 보관한다.
 * Solapi 발송 시 from/to 모두 이 번호를 사용한다.
 */
@Entity
@Table(name = "admin_setting")
@Getter
@NoArgsConstructor
public class AdminSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 관리자 전화번호 (알림 수신 = 발신) */
    private String adminPhoneNumber;

    /** 테마 프리셋 id (blue/green/warm/indigo) */
    @Column(length = 20)
    private String themeId = "blue";

    /** 커스텀 기본색(hex). 지정 시 프리셋보다 우선 */
    @Column(length = 20)
    private String primaryColor;

    public void changeAdminPhoneNumber(String phoneNumber) {
        this.adminPhoneNumber = (phoneNumber == null || phoneNumber.isBlank()) ? null : phoneNumber.trim();
    }

    public void changeTheme(String themeId, String primaryColor) {
        this.themeId = (themeId == null || themeId.isBlank()) ? "blue" : themeId.trim();
        this.primaryColor = (primaryColor == null || primaryColor.isBlank()) ? null : primaryColor.trim();
    }
}
