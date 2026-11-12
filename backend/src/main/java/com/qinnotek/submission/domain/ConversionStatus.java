package com.qinnotek.submission.domain;

/** PDF 사본 생성 상태. */
public enum ConversionStatus {
    /** 변환 대기/진행 중 */
    PENDING,
    /** PDF 사본 사용 가능 (원본이 이미 PDF 인 경우 포함) */
    READY,
    /** 변환 실패 - 원본만 보관 */
    FAILED
}
