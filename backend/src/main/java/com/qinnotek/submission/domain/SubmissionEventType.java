package com.qinnotek.submission.domain;

/**
 * 처리 이력 종류.
 */
public enum SubmissionEventType {
    /** 고객 제출 (submitRound 가 2 이상이면 재제출) */
    SUBMITTED,
    /** 관리자 통과 */
    APPROVED,
    /** 관리자 반환 (사유, 반환한 파일 보관) */
    RETURNED,
    /** 고객에게 반환 안내 발송 */
    NOTICE_SENT,
    /** 반환 안내 발송 실패 (연락처·링크 없음, 발송 오류) */
    NOTICE_FAILED,
    /** 제출 링크 발급·재발급 */
    LINK_ISSUED,
    /** 제출 링크 끄기 */
    LINK_REVOKED,
    /** 제출 링크 문자 발송 */
    LINK_SMS_SENT
}
