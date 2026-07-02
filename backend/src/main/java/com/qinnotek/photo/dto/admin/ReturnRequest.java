package com.qinnotek.photo.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 반환 요청. 반환 사유 필수.
 */
public record ReturnRequest(
        @NotBlank(message = "반환 사유를 입력해주세요.")
        String reason
) {
}
