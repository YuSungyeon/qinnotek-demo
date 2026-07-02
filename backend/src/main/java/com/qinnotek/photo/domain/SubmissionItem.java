package com.qinnotek.photo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 기업별 제출 항목. 특정 기업에 지정된 하나의 요구 사진과 그 현재 상태/최신 제출본을 나타낸다.
 * (기업 x 요구사진) 조합은 유일하다.
 */
@Entity
@Table(
        name = "submission_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "requirement_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubmissionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id")
    private PhotoRequirement requirement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    /** 반환 사유 (RETURNED 일 때) */
    @Column(length = 1000)
    private String rejectReason;

    // --- 최신 제출본 정보 (항상 최신 1장만 유지) ---
    /** 저장 파일명(UUID) */
    private String storedFileName;
    /** 원본 파일명 */
    private String originalFileName;
    /** 저장 경로 */
    private String storagePath;
    /** 업로드 일시 */
    private LocalDateTime uploadedAt;

    public SubmissionItem(Company company, PhotoRequirement requirement) {
        this.company = company;
        this.requirement = requirement;
        this.status = SubmissionStatus.PENDING;
    }

    /** 사진 업로드/교체 - 기존 제출본은 새 것으로 대체. 상태(PENDING/RETURNED)는 제출 전까지 유지. */
    public void attachPhoto(String storedFileName, String originalFileName, String storagePath) {
        this.storedFileName = storedFileName;
        this.originalFileName = originalFileName;
        this.storagePath = storagePath;
        this.uploadedAt = LocalDateTime.now();
    }

    /** 고객이 제출 확정 → 검수 대기 상태로 전환(반환 사유 초기화) */
    public void submit() {
        this.status = SubmissionStatus.SUBMITTED;
        this.rejectReason = null;
    }

    /** 관리자 통과 */
    public void approve() {
        this.status = SubmissionStatus.APPROVED;
    }

    /** 관리자 반환 - 기존 사진 정보 삭제, 반환 사유 저장 */
    public void markReturned(String reason) {
        this.status = SubmissionStatus.RETURNED;
        this.rejectReason = reason;
        this.storedFileName = null;
        this.originalFileName = null;
        this.storagePath = null;
        this.uploadedAt = null;
    }

    public boolean hasPhoto() {
        return storedFileName != null;
    }

    /** 고객이 지금 제출해야 하는 대상 항목인지 (미제출 또는 반환) */
    public boolean isActionableByCustomer() {
        return status == SubmissionStatus.PENDING || status == SubmissionStatus.RETURNED;
    }
}
