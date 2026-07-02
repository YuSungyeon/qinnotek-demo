package com.qinnotek.photo.repository;

import com.qinnotek.photo.domain.AdminSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminSettingRepository extends JpaRepository<AdminSetting, Long> {

    /** 단일 설정 행 조회 */
    Optional<AdminSetting> findTopByOrderByIdAsc();
}
