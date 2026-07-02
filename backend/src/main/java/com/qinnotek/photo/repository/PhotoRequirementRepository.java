package com.qinnotek.photo.repository;

import com.qinnotek.photo.domain.PhotoRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhotoRequirementRepository extends JpaRepository<PhotoRequirement, Long> {

    boolean existsByName(String name);

    Optional<PhotoRequirement> findByName(String name);

    List<PhotoRequirement> findAllByOrderByNameAsc();
}
