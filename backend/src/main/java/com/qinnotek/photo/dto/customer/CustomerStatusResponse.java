package com.qinnotek.photo.dto.customer;

import java.util.List;

/**
 * 고객 전화번호 조회 결과.
 *
 * @param state  고객 화면 분기용 상태
 *               INITIAL(최초 제출) / UNDER_REVIEW(검수 중) / RETURNED(반환) / COMPLETED(완료)
 * @param message 화면에 표시할 안내 메시지
 * @param items  제출/표시 대상 사진 항목 (검수 중/완료 시 비어있을 수 있음)
 */
public record CustomerStatusResponse(
        Long companyId,
        String companyName,
        String state,
        String message,
        List<CustomerPhotoItemResponse> items
) {
    public enum State {
        INITIAL, UNDER_REVIEW, RETURNED, COMPLETED
    }
}
