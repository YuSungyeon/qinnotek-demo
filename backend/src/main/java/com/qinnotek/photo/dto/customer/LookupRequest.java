package com.qinnotek.photo.dto.customer;

import jakarta.validation.constraints.NotBlank;

/**
 * 고객 전화번호 조회 요청.
 */
public record LookupRequest(
        @NotBlank(message = "전화번호를 입력해주세요.")
        String phoneNumber
) {
}
