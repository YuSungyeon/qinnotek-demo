package com.qinnotek.photo.controller;

import com.qinnotek.photo.dto.admin.RequirementResponse;
import com.qinnotek.photo.service.PhotoRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "관리자-요구사진", description = "관리자 요구 사진(마스터) 관리 API")
@RestController
@RequestMapping("/api/admin/requirements")
@RequiredArgsConstructor
public class AdminRequirementController {

    private final PhotoRequirementService requirementService;

    @Operation(summary = "요구 사진 목록")
    @GetMapping
    public List<RequirementResponse> list() {
        return requirementService.findAll();
    }

    @Operation(summary = "요구 사진 등록", description = "사진 명칭(중복 불가), 설명, AI 분류 힌트(영문), 예시 이미지")
    @PostMapping(consumes = "multipart/form-data")
    public RequirementResponse create(@RequestParam String name,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String classificationHint,
                                      @RequestParam(required = false) MultipartFile exampleImage) {
        return requirementService.create(name, description, classificationHint, exampleImage);
    }

    @Operation(summary = "요구 사진 수정")
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public RequirementResponse update(@PathVariable Long id,
                                      @RequestParam String name,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String classificationHint,
                                      @RequestParam(required = false) MultipartFile exampleImage) {
        return requirementService.update(id, name, description, classificationHint, exampleImage);
    }

    @Operation(summary = "요구 사진 삭제", description = "기업에 지정되지 않은 경우에만 삭제 가능")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        requirementService.delete(id);
    }
}
