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

    /**
     * 받는 파일 유형. nullable 로 두고 getter 에서 PHOTO 를 기본값으로 준다.
     * ddl-auto:update 는 NOT NULL 컬럼을 기존 행이 있는 테이블에 추가할 때 빈 문자열을 채워 넣어
     * enum 역직렬화가 깨지므로, 하위호환은 반드시 이 방식으로 처리한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RequirementFileType fileType;

    /** 문서 자동 분류용 키워드(쉼표 구분). 예: 사업자등록증,등록번호,대표자 */
    @Column(length = 500)
    private String documentKeywords;

    public PhotoRequirement(String name, String description, String classificationHint, String exampleImageFileName) {
        this(name, description, classificationHint, exampleImageFileName, RequirementFileType.PHOTO, null);
    }

    public PhotoRequirement(String name, String description, String classificationHint, String exampleImageFileName,
                            RequirementFileType fileType, String documentKeywords) {
        this.name = name;
        this.description = description;
        this.classificationHint = normalizeHint(classificationHint);
        this.exampleImageFileName = exampleImageFileName;
        this.fileType = fileType == null ? RequirementFileType.PHOTO : fileType;
        this.documentKeywords = normalizeHint(documentKeywords);
    }

    /** Lombok @Getter 대신 수동 정의 - 레거시 행(null)을 PHOTO 로 승격한다. */
    public RequirementFileType getFileType() {
        return fileType == null ? RequirementFileType.PHOTO : fileType;
    }

    public void update(String name, String description, String classificationHint) {
        this.name = name;
        this.description = description;
        this.classificationHint = normalizeHint(classificationHint);
    }

    public void update(String name, String description, String classificationHint,
                       RequirementFileType fileType, String documentKeywords) {
        update(name, description, classificationHint);
        this.fileType = fileType == null ? RequirementFileType.PHOTO : fileType;
        this.documentKeywords = normalizeHint(documentKeywords);
    }

    private static String normalizeHint(String hint) {
        return (hint == null || hint.isBlank()) ? null : hint.trim();
    }

    public void changeExampleImage(String exampleImageFileName) {
        this.exampleImageFileName = exampleImageFileName;
    }
}
