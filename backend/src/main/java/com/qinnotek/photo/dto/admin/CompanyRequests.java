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

    /** 제출 링크 발급. validDays 가 null 이면 기한 없음 */
    public record IssueLink(
            Integer validDays
    ) {
    }

    /** 제출 링크 문자 발송. url 은 화면에 보이는 링크 그대로 */
    public record SendLink(
            @NotBlank(message = "받는 번호를 입력해주세요.")
            String to,
            @NotBlank(message = "링크가 필요합니다.")
            String url
    ) {
    }

    public record AssignRequirements(
            List<Long> requirementIds
    ) {
    }
}
