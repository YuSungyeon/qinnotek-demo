package com.qinnotek.submission.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 전역 설정(단일 행). 지금은 제출 알림 문자 on/off 만 보관한다.
 * (알림 담당자는 Manager/CompanyManager 로 분리 관리)
 *
 * <p>예전의 디자인/테마 컬럼(design_id, theme_id, primary_color)은 화면을 모노크롬 하나로 고정하면서
 * 없앴다 - V3__drop_admin_setting_theme_columns.sql
 */
@Entity
@Table(name = "admin_setting")
@Getter
@NoArgsConstructor
public class AdminSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 문자 발송 on/off (관리자 토글). 기본 on, 실제 발송은 서버 키 설정도 필요 */
    @Column(nullable = false)
    private boolean smsEnabled = true;

    public void changeSmsEnabled(boolean enabled) {
        this.smsEnabled = enabled;
    }
}
