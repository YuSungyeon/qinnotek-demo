package com.qinnotek.photo.repository;

import com.qinnotek.photo.domain.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManagerRepository extends JpaRepository<Manager, Long> {

    List<Manager> findAllByOrderByNameAsc();
}
