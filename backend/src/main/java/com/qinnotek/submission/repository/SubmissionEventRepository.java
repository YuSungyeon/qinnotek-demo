package com.qinnotek.submission.repository;

import com.qinnotek.submission.domain.SubmissionEvent;
import com.qinnotek.submission.domain.SubmissionEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionEventRepository extends JpaRepository<SubmissionEvent, Long> {

    /** 기업 이력 - 최신이 먼저 */
    List<SubmissionEvent> findByCompanyIdOrderByIdDesc(Long companyId);

    long countByItemIdAndType(Long itemId, SubmissionEventType type);

    /** 반환 안내를 기다리는 반환 */
    List<SubmissionEvent> findByCompanyIdAndTypeAndNotifyPendingTrue(Long companyId, SubmissionEventType type);

    List<SubmissionEvent> findByItemIdAndTypeAndNotifyPendingTrue(Long itemId, SubmissionEventType type);

    /** 이 파일을 이력이 참조하는지 - 참조하면 항목을 지워도 파일은 남긴다 */
    boolean existsByStoredFileNameOrPdfFileName(String storedFileName, String pdfFileName);

    /** 가장 최근 반환 안내 실패 */
    SubmissionEvent findTopByCompanyIdAndTypeOrderByIdDesc(Long companyId, SubmissionEventType type);
}
