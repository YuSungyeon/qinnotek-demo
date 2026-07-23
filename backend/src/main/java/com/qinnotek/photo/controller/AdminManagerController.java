package com.qinnotek.photo.controller;

import com.qinnotek.photo.dto.admin.ManagerDto;
import com.qinnotek.photo.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자-담당자", description = "알림 담당자(이름·직책·전화번호) 관리")
@RestController
@RequestMapping("/api/admin/managers")
@RequiredArgsConstructor
public class AdminManagerController {

    private final ManagerService managerService;

    @Operation(summary = "담당자 목록")
    @GetMapping
    public List<ManagerDto.Response> list() {
        return managerService.findAll();
    }

    @Operation(summary = "담당자 등록")
    @PostMapping
    public ManagerDto.Response create(@Valid @RequestBody ManagerDto.Upsert request) {
        return managerService.create(request);
    }

    @Operation(summary = "담당자 수정")
    @PutMapping("/{id}")
    public ManagerDto.Response update(@PathVariable Long id, @Valid @RequestBody ManagerDto.Upsert request) {
        return managerService.update(id, request);
    }

    @Operation(summary = "담당자 삭제", description = "기업 지정 연결도 함께 제거")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        managerService.delete(id);
    }
}
