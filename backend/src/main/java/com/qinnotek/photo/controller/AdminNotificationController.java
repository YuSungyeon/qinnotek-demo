package com.qinnotek.photo.controller;

import com.qinnotek.photo.domain.SubmissionStatus;
import com.qinnotek.photo.repository.SubmissionItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 관리자 사이드바 알림용 경량 카운트 API (폴링 대상).
 * 목록/이미지 없이 COUNT/MAX만 조회 → 부담 거의 없음.
 */
@Tag(name = "관리자-알림", description = "검수 대기 건수 조회(폴링용)")
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final SubmissionItemRepository submissionItemRepository;

    public record Notifications(long reviewCompanies, long pendingItems, LocalDateTime latestSubmittedAt) {
    }

    @Operation(summary = "검수 대기 요약", description = "검수 대기 기업 수 / 사진 수 / 최근 제출 시각")
    @GetMapping
    @Transactional(readOnly = true)
    public Notifications get() {
        return new Notifications(
                submissionItemRepository.countDistinctCompaniesByStatus(SubmissionStatus.SUBMITTED),
                submissionItemRepository.countByStatus(SubmissionStatus.SUBMITTED),
                submissionItemRepository.findLatestUploadedAtByStatus(SubmissionStatus.SUBMITTED)
        );
    }
}
