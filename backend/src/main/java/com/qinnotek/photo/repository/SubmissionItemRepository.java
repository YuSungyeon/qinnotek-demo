package com.qinnotek.photo.repository;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.SubmissionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubmissionItemRepository extends JpaRepository<SubmissionItem, Long> {

    List<SubmissionItem> findByCompanyOrderByIdAsc(Company company);

    List<SubmissionItem> findByCompanyId(Long companyId);

    @Query("select i from SubmissionItem i join fetch i.requirement where i.company.id = :companyId order by i.id asc")
    List<SubmissionItem> findByCompanyIdWithRequirement(Long companyId);

    boolean existsByRequirementId(Long requirementId);
}
