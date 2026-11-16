package com.qinnotek.submission.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 반환 요청. 반환 사유 필수.
 *
 * @param notifyCustomer 검수를 마치면 고객에게 반환 안내를 보낼지 (비우면 보낸다)
 */
public record ReturnRequest(
        @NotBlank(message = "반환 사유를 입력해주세요.")
        String reason,
        Boolean notifyCustomer
) {
    public boolean notifyOrDefault() {
        return notifyCustomer == null || notifyCustomer;
    }
}
