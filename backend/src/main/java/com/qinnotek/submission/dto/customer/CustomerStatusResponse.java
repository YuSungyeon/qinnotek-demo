package com.qinnotek.submission.dto.customer;

import java.util.List;

/**
 * 고객 제출 링크를 열었을 때의 상태.
 *
 * @param state          고객 화면 분기용 상태
 *                       INITIAL(올릴 항목 있음) / RETURNED(반환된 항목만 남음) / UNDER_REVIEW(낼 것 없이 검수 중) / COMPLETED(완료)
 * @param message        화면에 표시할 안내 메시지
 * @param items          지금 올릴 항목 - 반환된 항목 먼저, 이어서 아직 안 낸 항목 (검수 중/완료 시 비어있음)
 * @param reviewingNames 먼저 보내 검수를 기다리는 항목 이름 (부분 제출 안내용)
 * @param approvedCount  통과된 항목 수 (부분 제출 안내용)
 */
public record CustomerStatusResponse(
        Long companyId,
        String companyName,
        String state,
        String message,
        List<CustomerItemResponse> items,
        List<String> reviewingNames,
        int approvedCount
) {
    public enum State {
        INITIAL, UNDER_REVIEW, RETURNED, COMPLETED
    }
}
