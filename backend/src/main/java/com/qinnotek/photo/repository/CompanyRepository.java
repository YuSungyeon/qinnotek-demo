package com.qinnotek.photo.repository;

import com.qinnotek.photo.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    List<Company> findByNameContainingOrderByNameAsc(String name);

    List<Company> findByNameContainingOrPhoneNumberContainingOrderByNameAsc(String name, String phoneNumber);

    List<Company> findAllByOrderByNameAsc();
}
