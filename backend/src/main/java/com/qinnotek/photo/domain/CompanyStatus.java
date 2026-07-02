package com.qinnotek.photo.domain;

import java.util.Collection;

/**
 * 기업 단위의 종합 상태(관리자 목록 화면 표시용). 개별 항목 상태로부터 계산된다.
 */
public enum CompanyStatus {
    /** 요구 사진이 지정되지 않음 */
    NONE("요청 없음", "#9ca3af"),
    /** 제출 대기 - 지정됐으나 아직 제출 전 */
    SUBMIT_WAITING("제출 대기", "#6b7280"),
    /** 검수 중 - 제출됐고 검수 대기 */
    UNDER_REVIEW("검수 중", "#2563eb"),
    /** 일부 반환 - 반환된 항목이 있어 고객 재제출 필요 */
    PARTIALLY_RETURNED("일부 반환", "#dc2626"),
    /** 완료 - 모든 항목 통과 */
    COMPLETED("완료", "#16a34a");

    private final String label;
    private final String color;

    CompanyStatus(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    /**
     * 항목 상태 목록으로부터 기업 종합 상태를 계산한다.
     */
    public static CompanyStatus from(Collection<SubmissionItem> items) {
        if (items == null || items.isEmpty()) {
            return NONE;
        }
        boolean anyReturned = items.stream().anyMatch(i -> i.getStatus() == SubmissionStatus.RETURNED);
        if (anyReturned) {
            return PARTIALLY_RETURNED;
        }
        boolean anySubmitted = items.stream().anyMatch(i -> i.getStatus() == SubmissionStatus.SUBMITTED);
        if (anySubmitted) {
            return UNDER_REVIEW;
        }
        boolean anyPending = items.stream().anyMatch(i -> i.getStatus() == SubmissionStatus.PENDING);
        if (anyPending) {
            return SUBMIT_WAITING;
        }
        return COMPLETED;
    }
}
