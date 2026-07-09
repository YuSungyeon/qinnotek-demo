package com.qinnotek.photo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 요구 사진(마스터). 관리자가 등록하는 촬영 항목 정의.
 * 사진 명칭은 중복 불가(unique).
 */
@Entity
@Table(name = "photo_requirement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사진 명칭 (중복 불가) */
    @Column(nullable = false, unique = true)
    private String name;

    /** 사진 설명 */
    @Column(length = 1000)
    private String description;

    /** AI 자동 분류용 영문 힌트 (예: building exterior). null 허용 */
    @Column(length = 200)
    private String classificationHint;

    /** 예시 이미지 저장 파일명(UUID). null 허용 */
    private String exampleImageFileName;

    public PhotoRequirement(String name, String description, String classificationHint, String exampleImageFileName) {
        this.name = name;
        this.description = description;
        this.classificationHint = normalizeHint(classificationHint);
        this.exampleImageFileName = exampleImageFileName;
    }

    public void update(String name, String description, String classificationHint) {
        this.name = name;
        this.description = description;
        this.classificationHint = normalizeHint(classificationHint);
    }

    private static String normalizeHint(String hint) {
        return (hint == null || hint.isBlank()) ? null : hint.trim();
    }

    public void changeExampleImage(String exampleImageFileName) {
        this.exampleImageFileName = exampleImageFileName;
    }
}
