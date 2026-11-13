package com.qinnotek.submission.service;

import com.qinnotek.submission.domain.Requirement;
import com.qinnotek.submission.domain.RequirementFileType;
import com.qinnotek.submission.dto.admin.RequirementResponse;
import com.qinnotek.submission.exception.BusinessException;
import com.qinnotek.submission.repository.RequirementRepository;
import com.qinnotek.submission.repository.SubmissionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 요청 자료(마스터) 관리.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final FileStorageService fileStorageService;

    public List<RequirementResponse> findAll() {
        // 지정 기업 수를 한 번의 집계 쿼리로 가져온다 (항목마다 세면 요청 자료 수만큼 쿼리가 나간다)
        Map<Long, Long> assigned = submissionItemRepository.countGroupByRequirement().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return requirementRepository.findAllByOrderByNameAsc().stream()
                .map(r -> RequirementResponse.from(r, assigned.getOrDefault(r.getId(), 0L)))
                .toList();
    }

    public Requirement getOrThrow(Long id) {
        return requirementRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("요청 자료를 찾을 수 없습니다."));
    }

    @Transactional
    public RequirementResponse create(String name, String description, String classificationHint,
                                      String fileType, String documentKeywords,
                                      MultipartFile exampleImage) {
        if (requirementRepository.existsByName(name)) {
            throw BusinessException.conflict("이미 존재하는 자료 명칭입니다: " + name);
        }
        String exampleFileName = null;
        if (exampleImage != null && !exampleImage.isEmpty()) {
            exampleFileName = fileStorageService.store(exampleImage).storedFileName();
        }
        Requirement saved = requirementRepository.save(
                new Requirement(name, description, classificationHint, exampleFileName,
                        RequirementFileType.from(fileType), documentKeywords));
        return RequirementResponse.from(saved);
    }

    @Transactional
    public RequirementResponse update(Long id, String name, String description, String classificationHint,
                                      String fileType, String documentKeywords,
                                      MultipartFile exampleImage) {
        Requirement req = getOrThrow(id);
        if (!req.getName().equals(name) && requirementRepository.existsByName(name)) {
            throw BusinessException.conflict("이미 존재하는 자료 명칭입니다: " + name);
        }
        req.update(name, description, classificationHint, RequirementFileType.from(fileType), documentKeywords);
        if (exampleImage != null && !exampleImage.isEmpty()) {
            String old = req.getExampleImageFileName();
            String newFile = fileStorageService.store(exampleImage).storedFileName();
            req.changeExampleImage(newFile);
            fileStorageService.delete(old);
        }
        return RequirementResponse.from(req);
    }

    @Transactional
    public void delete(Long id) {
        Requirement req = getOrThrow(id);
        if (submissionItemRepository.existsByRequirementId(id)) {
            throw BusinessException.conflict("이미 기업에 지정된 요청 자료는 삭제할 수 없습니다.");
        }
        fileStorageService.delete(req.getExampleImageFileName());
        requirementRepository.delete(req);
    }
}
