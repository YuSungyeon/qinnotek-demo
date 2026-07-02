package com.qinnotek.photo.dto.admin;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 기업 관련 요청 DTO 모음.
 */
public class CompanyRequests {

    public record CreateCompany(
            @NotBlank(message = "기업명을 입력해주세요.")
            String name
    ) {
    }

    public record UpdateName(
            @NotBlank(message = "기업명을 입력해주세요.")
            String name
    ) {
    }

    public record UpdatePhone(
            String phoneNumber
    ) {
    }

    public record AssignRequirements(
            List<Long> requirementIds
    ) {
    }
}
