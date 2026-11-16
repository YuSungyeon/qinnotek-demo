package com.qinnotek.submission.service;

import com.qinnotek.submission.domain.SubmissionEvent;
import com.qinnotek.submission.domain.SubmissionEventType;
import com.qinnotek.submission.domain.SubmissionItem;
import com.qinnotek.submission.dto.admin.SubmissionEventResponse;
import com.qinnotek.submission.repository.SubmissionEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 처리 이력 - 제출·재제출·통과·반환·링크 발급을 쌓고 보여준다. 이력은 기업을 삭제할 때만 지운다.
 * (반환 안내 발송 이력은 ReturnNoticeService 가 쌓는다)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionHistoryService {

    private final SubmissionEventRepository eventRepository;
    private final FileStorageService fileStorageService;

    /** 기업 이력 - 최신이 먼저 */
    public List<SubmissionEventResponse> list(Long companyId) {
        return eventRepository.findByCompanyIdOrderByIdDesc(companyId).stream()
                .map(SubmissionEventResponse::from)
                .toList();
    }

    /** 고객 제출 - 한 번에 낸 항목은 같은 시각으로 남겨 화면에서 한 줄로 묶는다 */
    @Transactional
    public void recordSubmitted(List<SubmissionItem> items) {
        LocalDateTime at = LocalDateTime.now();
        for (SubmissionItem item : items) {
            int round = (int) eventRepository.countByItemIdAndType(item.getId(), SubmissionEventType.SUBMITTED) + 1;
            eventRepository.save(SubmissionEvent.submitted(item, round, at));
            // 고객이 다시 냈으니 이 항목의 반환 안내는 더 보낼 필요가 없다
            eventRepository.findByItemIdAndTypeAndNotifyPendingTrue(item.getId(), SubmissionEventType.RETURNED)
                    .forEach(SubmissionEvent::clearNotifyPending);
        }
    }

    @Transactional
    public void recordApproved(SubmissionItem item) {
        eventRepository.save(SubmissionEvent.approved(item, LocalDateTime.now()));
    }

    /** 반환 - item.markReturned() 로 파일 정보가 지워지기 전에 불러야 반환한 파일이 이력에 남는다 */
    @Transactional
    public void recordReturned(SubmissionItem item, String reason, boolean notify) {
        eventRepository.save(SubmissionEvent.returned(item, reason, notify, LocalDateTime.now()));
    }

    @Transactional
    public void recordLink(Long companyId, SubmissionEventType type, String recipient, String detail) {
        eventRepository.save(SubmissionEvent.link(companyId, type, recipient, detail, LocalDateTime.now()));
    }

    /** 이력이 이 파일을 참조하는지 - 참조하면 항목을 지워도 파일은 남긴다 */
    public boolean isReferenced(String fileName) {
        return fileName != null && eventRepository.existsByStoredFileNameOrPdfFileName(fileName, fileName);
    }

    /** 기업 삭제 - 이력과 이력이 보관하던 파일(반환본 포함)을 함께 지운다 */
    @Transactional
    public void deleteCompanyHistory(Long companyId) {
        List<SubmissionEvent> events = eventRepository.findByCompanyIdOrderByIdDesc(companyId);
        for (SubmissionEvent e : events) {
            fileStorageService.delete(e.getStoredFileName());
            fileStorageService.delete(e.getPdfFileName());
        }
        eventRepository.deleteAll(events);
    }
}
