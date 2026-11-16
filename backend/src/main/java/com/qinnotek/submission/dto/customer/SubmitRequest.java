package com.qinnotek.submission.dto.customer;

import java.util.List;

/**
 * 최종 제출 요청.
 *
 * @param itemIds 이번에 낼 항목. 일부만 먼저 낼 수 있다. 비우면(null) 파일을 올려 둔 항목을 모두 낸다.
 */
public record SubmitRequest(List<Long> itemIds) {
}
