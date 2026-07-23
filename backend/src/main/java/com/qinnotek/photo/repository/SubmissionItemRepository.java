package com.qinnotek.photo.repository;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.SubmissionItem;
import com.qinnotek.photo.domain.SubmissionStatus;
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

    // --- 알림 배지용 경량 카운트 ---
    long countByStatus(SubmissionStatus status);

    @Query("select count(distinct i.company.id) from SubmissionItem i where i.status = :status")
    long countDistinctCompaniesByStatus(@Param("status") SubmissionStatus status);

    @Query("select max(i.uploadedAt) from SubmissionItem i where i.status = :status")
    LocalDateTime findLatestUploadedAtByStatus(@Param("status") SubmissionStatus status);
}
