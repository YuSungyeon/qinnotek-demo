package com.qinnotek.submission.repository;

import com.qinnotek.submission.domain.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {

    boolean existsByName(String name);

    Optional<Requirement> findByName(String name);

    List<Requirement> findAllByOrderByNameAsc();
}
