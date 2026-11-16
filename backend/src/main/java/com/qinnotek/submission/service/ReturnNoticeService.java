package com.qinnotek.submission.service;

import com.qinnotek.submission.config.AppProperties;
import com.qinnotek.submission.domain.Company;
import com.qinnotek.submission.domain.SubmissionEvent;
import com.qinnotek.submission.domain.SubmissionEventType;
import com.qinnotek.submission.domain.SubmissionItem;
import com.qinnotek.submission.domain.SubmissionStatus;
import com.qinnotek.submission.exception.BusinessException;
import com.qinnotek.submission.repository.SubmissionEventRepository;
import com.qinnotek.submission.repository.SubmissionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 반환 안내 - 반환할 때마다 보내지 않고, 그 기업의 검수 대기가 0이 되는 순간 반환 항목을 모아 문자 1통으로 보낸다.
 * 받는 번호는 기업 연락처, 링크는 지금 쓰는 제출 링크. 둘 중 하나라도 없으면 보내지 않고 실패로 남겨
 * 관리자 화면에서 다시 보내게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnNoticeService {

    private final SubmissionEventRepository eventRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final SmsService smsService;
    private final AppProperties appProperties;

    /**
     * @param sent    보냈는지
     * @param to      받는 번호 (없으면 null)
     * @param count   안내한 반환 항목 수
     * @param message 보내지 못한 이유 (보냈으면 null)
     */
    public record Result(boolean sent, String to, int count, String message) {
    }

    /** 관리자 화면 표시용 - 검수를 마쳤는데 아직 못 보낸 반환 안내가 있는지 */
    public record Status(boolean pending, String message) {
    }

    /** 검수 대기가 0이 됐고 안내를 기다리는 반환이 있으면 모아서 보낸다. 보낼 게 없으면 null */
    @Transactional
    public Result sendIfReviewFinished(Company company) {
        List<SubmissionItem> items = submissionItemRepository.findByCompanyIdWithRequirement(company.getId());
        if (!reviewFinished(items) || pendingReturns(company.getId()).isEmpty()) {
            return null;
        }
        return send(company, items);
    }

    /** 지금 반환 상태인 항목을 모아 안내한다 (자동 발송, 관리자의 '다시 보내기' 공용) */
    @Transactional
    public Result send(Company company) {
        return send(company, submissionItemRepository.findByCompanyIdWithRequirement(company.getId()));
    }

    public Status status(Long companyId, List<SubmissionItem> items) {
        if (!reviewFinished(items) || pendingReturns(companyId).isEmpty()) {
            return new Status(false, null);
        }
        SubmissionEvent lastFailure =
                eventRepository.findTopByCompanyIdAndTypeOrderByIdDesc(companyId, SubmissionEventType.NOTICE_FAILED);
        return new Status(true, lastFailure != null ? lastFailure.getDetail() : "반환 안내를 아직 보내지 않았어요.");
    }

    private Result send(Company company, List<SubmissionItem> items) {
        List<SubmissionItem> returned = items.stream()
                .filter(i -> i.getStatus() == SubmissionStatus.RETURNED)
                .toList();
        if (returned.isEmpty()) {
            throw BusinessException.badRequest("반환된 항목이 없습니다.");
        }
        String to = company.getPhoneNumber() == null ? "" : company.getPhoneNumber().replaceAll("\\D", "");
        LocalDateTime now = LocalDateTime.now();

        String failure = null;
        if (to.isEmpty()) {
            failure = "고객 연락처가 없어요.";
        } else if (company.getAccessToken() == null || company.isLinkExpired(now)) {
            failure = "제출 링크가 꺼져 있거나 기간이 지났어요.";
        } else {
            try {
                smsService.sendReturnNotice(to, company.getName(),
                        returned.stream()
                                .map(i -> new SmsService.ReturnLine(i.getRequirement().getName(), i.getRejectReason()))
                                .toList(),
                        appProperties.linkUrl(company.getAccessToken()));
            } catch (BusinessException e) {
                failure = e.getMessage();
            }
        }

        boolean sent = failure == null;
        String names = returned.stream().map(i -> i.getRequirement().getName()).collect(Collectors.joining(" · "));
        eventRepository.save(SubmissionEvent.notice(company.getId(), sent, to.isEmpty() ? null : to,
                sent ? names : failure, now));
        if (sent) {
            pendingReturns(company.getId()).forEach(SubmissionEvent::clearNotifyPending);
        }
        return new Result(sent, to.isEmpty() ? null : to, returned.size(), failure);
    }

    private static boolean reviewFinished(List<SubmissionItem> items) {
        return items.stream().noneMatch(i -> i.getStatus() == SubmissionStatus.SUBMITTED);
    }

    private List<SubmissionEvent> pendingReturns(Long companyId) {
        return eventRepository.findByCompanyIdAndTypeAndNotifyPendingTrue(companyId, SubmissionEventType.RETURNED);
    }
}
