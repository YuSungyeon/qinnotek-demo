package com.qinnotek.submission.domain;

/** 스테이징된 문서의 생명주기. */
public enum StagedState {
    /** 업로드됨 - 아직 어느 항목에도 확정되지 않음 */
    STAGED,
    /** 제출 항목에 확정됨 - 파일 소유권이 SubmissionItem 으로 넘어감 */
    CONSUMED
}
