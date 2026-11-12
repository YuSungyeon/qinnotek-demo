package com.qinnotek.submission.domain;

import lombok.Getter;

/**
 * 요청 자료이 받는 파일 유형. 고객 화면의 입력 방식(카메라/파일선택)과 자동 분류 대상 슬롯을 가른다.
 */
@Getter
public enum RequirementFileType {

    /** 사진만 - 브라우저 CLIP 자동 분류 대상 */
    PHOTO("사진"),
    /** 문서만 - 서버 텍스트 분류 대상 */
    DOCUMENT("문서"),
    /** 사진/문서 모두 허용 */
    ANY("무관");

    private final String label;

    RequirementFileType(String label) {
        this.label = label;
    }

    public boolean acceptsPhoto() {
        return this != DOCUMENT;
    }

    public boolean acceptsDocument() {
        return this != PHOTO;
    }

    /** 잘못된 값/빈 값은 PHOTO 로 (기존 동작 유지). */
    public static RequirementFileType from(String value) {
        if (value == null || value.isBlank()) {
            return PHOTO;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PHOTO;
        }
    }
}
