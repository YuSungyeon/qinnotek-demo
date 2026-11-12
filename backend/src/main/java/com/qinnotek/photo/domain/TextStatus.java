package com.qinnotek.photo.domain;

/** 문서에서 분류용 텍스트를 뽑은 결과. */
public enum TextStatus {
    /** 정상 추출 */
    OK,
    /** 텍스트 레이어 없음(스캔 PDF) - 파일명으로만 분류 가능 */
    NO_TEXT,
    /** 추출 중 오류 */
    ERROR
}
