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

    public void changeAdminPhoneNumber(String phoneNumber) {
        this.adminPhoneNumber = (phoneNumber == null || phoneNumber.isBlank()) ? null : phoneNumber.trim();
    }
}
