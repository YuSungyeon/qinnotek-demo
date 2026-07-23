package com.qinnotek.photo.service;

import com.qinnotek.photo.domain.Manager;
import com.qinnotek.photo.dto.admin.ManagerDto;
import com.qinnotek.photo.exception.BusinessException;
import com.qinnotek.photo.repository.CompanyManagerRepository;
import com.qinnotek.photo.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 담당자(마스터) 관리.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final CompanyManagerRepository companyManagerRepository;

    public List<ManagerDto.Response> findAll() {
        return managerRepository.findAllByOrderByNameAsc().stream()
                .map(ManagerDto.Response::from)
                .toList();
    }

    @Transactional
    public ManagerDto.Response create(ManagerDto.Upsert req) {
        Manager saved = managerRepository.save(
                new Manager(req.name(), req.position(), req.phoneNumber()));
        return ManagerDto.Response.from(saved);
    }

    @Transactional
    public ManagerDto.Response update(Long id, ManagerDto.Upsert req) {
        Manager m = managerRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("담당자를 찾을 수 없습니다."));
        m.update(req.name(), req.position(), req.phoneNumber());
        return ManagerDto.Response.from(m);
    }

    /** 담당자 삭제 - 기업 지정 연결도 함께 제거 */
    @Transactional
    public void delete(Long id) {
        Manager m = managerRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("담당자를 찾을 수 없습니다."));
        companyManagerRepository.deleteByManagerId(id);
        managerRepository.delete(m);
    }
}
