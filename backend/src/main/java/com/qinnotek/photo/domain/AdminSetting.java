package com.qinnotek.photo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 전역 설정(단일 행). 디자인/테마 설정을 보관한다.
 * (알림 담당자는 Manager/CompanyManager로 분리 관리)
 */
@Entity
@Table(name = "admin_setting")
@Getter
@NoArgsConstructor
public class AdminSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 기업 디자인 팩 id (base/apple/figma/airtable) */
    @Column(length = 20)
    private String designId = "base";

    /** 테마 프리셋 id (blue/green/warm/indigo) - base 디자인의 강조색 */
    @Column(length = 20)
    private String themeId = "blue";

    /** 커스텀 기본색(hex). 지정 시 프리셋보다 우선 */
    @Column(length = 20)
    private String primaryColor;

    /** 문자 발송 on/off (관리자 토글). 기본 on, 실제 발송은 서버 키 설정도 필요 */
    @Column(nullable = false)
    private boolean smsEnabled = true;

    public void changeTheme(String designId, String themeId, String primaryColor) {
        this.designId = (designId == null || designId.isBlank()) ? "base" : designId.trim();
        this.themeId = (themeId == null || themeId.isBlank()) ? "blue" : themeId.trim();
        this.primaryColor = (primaryColor == null || primaryColor.isBlank()) ? null : primaryColor.trim();
    }

    public void changeSmsEnabled(boolean enabled) {
        this.smsEnabled = enabled;
    }
}
