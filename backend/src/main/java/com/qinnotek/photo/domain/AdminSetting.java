package com.qinnotek.photo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자 전역 설정(단일 행). 알림 문자를 받을 관리자 전화번호(들)를 보관한다.
 * 여러 번호는 쉼표로 구분해 한 컬럼에 저장한다.
 */
@Entity
@Table(name = "admin_setting")
@Getter
@NoArgsConstructor
public class AdminSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 관리자 전화번호(들). 쉼표 구분, 숫자만 저장 */
    @Column(length = 500)
    private String adminPhoneNumber;

    /** 기업 디자인 팩 id (base/apple/figma/airtable) */
    @Column(length = 20)
    private String designId = "base";

    /** 테마 프리셋 id (blue/green/warm/indigo) - base 디자인의 강조색 */
    @Column(length = 20)
    private String themeId = "blue";

    /** 커스텀 기본색(hex). 지정 시 프리셋보다 우선 */
    @Column(length = 20)
    private String primaryColor;

    /** 입력(줄바꿈/쉼표/공백 등 혼용)을 숫자만 남긴 번호 목록으로 정규화해 쉼표로 저장 */
    public void changeAdminPhoneNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            this.adminPhoneNumber = null;
            return;
        }
        String joined = Arrays.stream(raw.split("[,\\n;]"))
                .map(s -> s.replaceAll("\\D", ""))
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
        this.adminPhoneNumber = joined.isBlank() ? null : joined;
    }

    /** 저장된 번호 목록 */
    public List<String> getAdminPhoneNumbers() {
        if (adminPhoneNumber == null || adminPhoneNumber.isBlank()) {
            return List.of();
        }
        return Arrays.stream(adminPhoneNumber.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public void changeTheme(String designId, String themeId, String primaryColor) {
        this.designId = (designId == null || designId.isBlank()) ? "base" : designId.trim();
        this.themeId = (themeId == null || themeId.isBlank()) ? "blue" : themeId.trim();
        this.primaryColor = (primaryColor == null || primaryColor.isBlank()) ? null : primaryColor.trim();
    }
}
