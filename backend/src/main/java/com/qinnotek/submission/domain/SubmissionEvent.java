package com.qinnotek.submission.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 처리 이력 한 줄. 쌓기만 하고 고치지 않는다 (반환 안내 대기 표시만 예외).
 * 처리자(누가)는 관리자 로그인이 붙으면 함께 기록한다.
 */
@Entity
@Table(name = "submission_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubmissionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    /** 제출 항목 id. 항목이 지워져도 이력은 남으므로 FK 없이 값만 둔다 */
    private Long itemId;

    /** 그 시점의 항목(요청 자료) 이름 */
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private SubmissionEventType type;

    /** 제출 차수 (SUBMITTED). 1 = 첫 제출, 2 이상 = 재제출 */
    private Integer submitRound;

    /** 반환 사유 (RETURNED) */
    @Column(length = 1000)
    private String reason;

    /** 그 시점의 제출 파일 (SUBMITTED / RETURNED). 반환한 파일도 지우지 않고 보관한다 */
    private String storedFileName;
    private String pdfFileName;

    @Enumerated(EnumType.STRING)
    private ContentKind contentKind;

    /** 문자 받는 번호 (NOTICE_* / LINK_SMS_SENT) */
    @Column(length = 30)
    private String recipient;

    /** 부가 설명 - 반환 안내에 담은 항목, 실패 사유, 링크 기한 등 */
    @Column(length = 1000)
    private String detail;

    /** 반환 안내를 보내야 하는 반환인지 (RETURNED). 검수를 다 마치면 모아 보내고 false 로 바꾼다 */
    @Column(nullable = false)
    private boolean notifyPending;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private SubmissionEvent(Long companyId, SubmissionEventType type, LocalDateTime createdAt) {
        this.companyId = companyId;
        this.type = type;
        this.createdAt = createdAt;
    }

    public static SubmissionEvent submitted(SubmissionItem item, int round, LocalDateTime at) {
        SubmissionEvent e = ofItem(item, SubmissionEventType.SUBMITTED, at);
        e.submitRound = round;
        e.copyFile(item);
        return e;
    }

    public static SubmissionEvent approved(SubmissionItem item, LocalDateTime at) {
        SubmissionEvent e = ofItem(item, SubmissionEventType.APPROVED, at);
        e.copyFile(item);
        return e;
    }

    /** 반환 - item.markReturned() 로 파일 정보가 지워지기 전에 만든다 */
    public static SubmissionEvent returned(SubmissionItem item, String reason, boolean notify, LocalDateTime at) {
        SubmissionEvent e = ofItem(item, SubmissionEventType.RETURNED, at);
        e.reason = reason;
        e.notifyPending = notify;
        e.copyFile(item);
        return e;
    }

    public static SubmissionEvent notice(Long companyId, boolean sent, String recipient, String detail, LocalDateTime at) {
        SubmissionEvent e = new SubmissionEvent(companyId,
                sent ? SubmissionEventType.NOTICE_SENT : SubmissionEventType.NOTICE_FAILED, at);
        e.recipient = recipient;
        e.detail = truncate(detail);
        return e;
    }

    public static SubmissionEvent link(Long companyId, SubmissionEventType type, String recipient, String detail,
                                       LocalDateTime at) {
        SubmissionEvent e = new SubmissionEvent(companyId, type, at);
        e.recipient = recipient;
        e.detail = truncate(detail);
        return e;
    }

    /** 반환 안내를 보냈거나(또는 고객이 다시 냈거나) 더 보낼 필요가 없을 때 */
    public void clearNotifyPending() {
        this.notifyPending = false;
    }

    private static SubmissionEvent ofItem(SubmissionItem item, SubmissionEventType type, LocalDateTime at) {
        SubmissionEvent e = new SubmissionEvent(item.getCompany().getId(), type, at);
        e.itemId = item.getId();
        e.itemName = item.getRequirement().getName();
        return e;
    }

    private void copyFile(SubmissionItem item) {
        if (item.hasFile()) {
            this.storedFileName = item.getStoredFileName();
            this.pdfFileName = item.getPdfFileName();
            this.contentKind = item.getContentKind();
        }
    }

    private static String truncate(String s) {
        return s == null || s.length() <= 1000 ? s : s.substring(0, 1000);
    }
}
