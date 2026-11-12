package com.qinnotek.submission.controller;

import com.qinnotek.submission.dto.admin.RequirementResponse;
import com.qinnotek.submission.service.RequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "관리자-요청자료", description = "관리자 요청 자료(마스터) 관리 API")
@RestController
@RequestMapping("/api/admin/requirements")
@RequiredArgsConstructor
public class AdminRequirementController {

    private final RequirementService requirementService;

    @Operation(summary = "요청 자료 목록")
    @GetMapping
    public List<RequirementResponse> list() {
        return requirementService.findAll();
    }

    @Operation(summary = "요청 자료 등록",
            description = "명칭(중복 불가), 설명, 파일 유형(PHOTO/DOCUMENT/ANY), 사진 분류 힌트(영문)와 예시 이미지, 문서 분류 키워드")
    @PostMapping(consumes = "multipart/form-data")
    public RequirementResponse create(@RequestParam String name,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String classificationHint,
                                      @RequestParam(required = false) String fileType,
                                      @RequestParam(required = false) String documentKeywords,
                                      @RequestParam(required = false) MultipartFile exampleImage) {
        return requirementService.create(name, description, classificationHint, fileType, documentKeywords, exampleImage);
    }

    @Operation(summary = "요청 자료 수정")
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public RequirementResponse update(@PathVariable Long id,
                                      @RequestParam String name,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String classificationHint,
                                      @RequestParam(required = false) String fileType,
                                      @RequestParam(required = false) String documentKeywords,
                                      @RequestParam(required = false) MultipartFile exampleImage) {
        return requirementService.update(id, name, description, classificationHint, fileType, documentKeywords, exampleImage);
    }

    @Operation(summary = "요청 자료 삭제", description = "기업에 지정되지 않은 경우에만 삭제 가능")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        requirementService.delete(id);
    }
}
