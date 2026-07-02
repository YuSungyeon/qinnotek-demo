package com.qinnotek.photo.dto.customer;

import jakarta.validation.constraints.NotNull;

/**
 * 고객 최종 제출 요청.
 */
public record SubmitRequest(
        @NotNull(message = "기업 정보가 필요합니다.")
        Long companyId
) {
}
