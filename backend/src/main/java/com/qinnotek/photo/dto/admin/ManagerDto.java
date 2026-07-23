package com.qinnotek.photo.dto.admin;

import com.qinnotek.photo.domain.Manager;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 담당자 관련 DTO 모음.
 */
public class ManagerDto {

    public record Response(Long id, String name, String position, String phoneNumber, List<String> companies) {
        public static Response from(Manager m, List<String> companies) {
            return new Response(m.getId(), m.getName(), m.getPosition(), m.getPhoneNumber(), companies);
        }
    }

    public record Upsert(
            @NotBlank(message = "이름을 입력해주세요.")
            String name,
            String position,
            @NotBlank(message = "전화번호를 입력해주세요.")
            String phoneNumber
    ) {
    }

    public record Assign(List<Long> managerIds) {
    }
}
