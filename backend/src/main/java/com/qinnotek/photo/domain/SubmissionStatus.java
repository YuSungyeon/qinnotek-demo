package com.qinnotek.photo.domain;

/**
 * 개별 사진 항목의 상태.
 */
public enum SubmissionStatus {
    /** 미제출 - 고객이 아직 사진을 올리지 않음 */
    PENDING("미제출"),
    /** 검수 대기 - 고객이 제출했고 관리자 검수 대기 중 */
    SUBMITTED("검수 대기"),
    /** 통과 - 관리자가 승인 */
    APPROVED("통과"),
    /** 반환 - 관리자가 반환, 고객 재촬영 필요 */
    RETURNED("반환");

    private final String label;

    SubmissionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
