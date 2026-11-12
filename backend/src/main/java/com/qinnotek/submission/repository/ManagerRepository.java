package com.qinnotek.submission.repository;

import com.qinnotek.submission.domain.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManagerRepository extends JpaRepository<Manager, Long> {

    List<Manager> findAllByOrderByNameAsc();
}
