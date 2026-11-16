package com.qinnotek.submission.repository;

import com.qinnotek.submission.domain.Company;
import com.qinnotek.submission.domain.SubmissionItem;
import com.qinnotek.submission.domain.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SubmissionItemRepository extends JpaRepository<SubmissionItem, Long> {

    List<SubmissionItem> findByCompanyOrderByIdAsc(Company company);

    List<SubmissionItem> findByCompanyId(Long companyId);

    @Query("select i from SubmissionItem i join fetch i.requirement where i.company.id = :companyId order by i.id asc")
    List<SubmissionItem> findByCompanyIdWithRequirement(Long companyId);

    boolean existsByRequirementId(Long requirementId);

    /** 저장 파일명(UUID)으로 그 파일을 들고 있는 항목을 찾는다 - 변환이 늦게 끝난 문서의 PDF 사본 반영용 */
    java.util.Optional<SubmissionItem> findByStoredFileName(String storedFileName);

    // --- 알림 배지용 경량 카운트 ---
    long countByStatus(SubmissionStatus status);

    /** 요청 자료별로 지정된 기업 수 - [requirementId, count] 목록 (요청 자료 관리 화면의 "기업 N곳에 지정") */
    @Query("select i.requirement.id, count(i) from SubmissionItem i group by i.requirement.id")
    List<Object[]> countGroupByRequirement();

    @Query("select count(distinct i.company.id) from SubmissionItem i where i.status = :status")
    long countDistinctCompaniesByStatus(@Param("status") SubmissionStatus status);

    @Query("select max(i.uploadedAt) from SubmissionItem i where i.status = :status")
    LocalDateTime findLatestUploadedAtByStatus(@Param("status") SubmissionStatus status);
}
