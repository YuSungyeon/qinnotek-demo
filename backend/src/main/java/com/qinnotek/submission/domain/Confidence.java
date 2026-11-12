package com.qinnotek.submission.domain;

/** 자동 분류 확신도. */
public enum Confidence {
    /** 그대로 써도 되는 수준 */
    HIGH,
    /** 배정은 했지만 고객이 확인해야 하는 수준 */
    LOW,
    /** 배정 포기 - 고객이 직접 항목을 골라야 한다 */
    NONE
}
