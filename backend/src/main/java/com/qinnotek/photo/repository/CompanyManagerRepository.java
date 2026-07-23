package com.qinnotek.photo.repository;

import com.qinnotek.photo.domain.CompanyManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CompanyManagerRepository extends JpaRepository<CompanyManager, Long> {

    @Query("select cm from CompanyManager cm join fetch cm.manager where cm.company.id = :companyId")
    List<CompanyManager> findByCompanyIdWithManager(Long companyId);

    List<CompanyManager> findByCompanyId(Long companyId);

    void deleteByManagerId(Long managerId);

    boolean existsByManagerId(Long managerId);
}
