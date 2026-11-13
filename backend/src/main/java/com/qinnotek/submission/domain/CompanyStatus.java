package com.qinnotek.submission.domain;

import java.util.Collection;

/**
 * 기업 단위의 종합 상태(관리자 목록 화면 표시용). 개별 항목 상태로부터 계산된다.
 *
 * <p>라벨은 "지금 누가 움직여야 하는가"로 짓는다: 검수 대기 = 관리자 차례, 재제출 대기 = 고객 차례.
 * 화면 색은 서버가 정하지 않는다. 프론트가 상태 키(name)로 칩 모양을 고른다 (frontend/src/lib/status.js).
 */
public enum CompanyStatus {
    /** 요청 자료가 지정되지 않음 */
    NONE("요청 없음"),
    /** 제출 대기 - 지정됐으나 아직 제출 전 */
    SUBMIT_WAITING("제출 대기"),
    /** 검수 대기 - 고객이 제출했고 관리자 검수를 기다림 */
    UNDER_REVIEW("검수 대기"),
    /** 재제출 대기 - 반환된 항목이 있어 고객 재제출을 기다림 */
    PARTIALLY_RETURNED("재제출 대기"),
    /** 완료 - 모든 항목 통과 */
    COMPLETED("완료");

    private final String label;

    CompanyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }


    /**
     * 항목 상태 목록으로부터 기업 종합 상태를 계산한다.
     */
    /**
     * 우선순위: 관리자가 할 일(검수 대기) → 고객이 할 일(재제출 대기) → 제출 대기 → 완료.
     * 반환한 항목이 있어도 아직 검수하지 않은 항목이 남아 있으면 "검수 대기"로 본다.
     * 관리자 상단 바의 검수 대기 기업 수(제출 상태 항목이 있는 기업 수)와 목록의 숫자가 같아지게 하려는 것이다.
     */
    public static CompanyStatus from(Collection<SubmissionItem> items) {
        if (items == null || items.isEmpty()) {
            return NONE;
        }
        boolean anySubmitted = items.stream().anyMatch(i -> i.getStatus() == SubmissionStatus.SUBMITTED);
        if (anySubmitted) {
            return UNDER_REVIEW;
        }
        boolean anyReturned = items.stream().anyMatch(i -> i.getStatus() == SubmissionStatus.RETURNED);
        if (anyReturned) {
            return PARTIALLY_RETURNED;
        }
        boolean anyPending = items.stream().anyMatch(i -> i.getStatus() == SubmissionStatus.PENDING);
        if (anyPending) {
            return SUBMIT_WAITING;
        }
        return COMPLETED;
    }
}
