package com.qinnotek.photo.service;

import com.qinnotek.photo.domain.PhotoRequirement;
import com.qinnotek.photo.dto.admin.RequirementResponse;
import com.qinnotek.photo.exception.BusinessException;
import com.qinnotek.photo.repository.PhotoRequirementRepository;
import com.qinnotek.photo.repository.SubmissionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 요구 사진(마스터) 관리.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoRequirementService {

    private final PhotoRequirementRepository requirementRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final FileStorageService fileStorageService;

    public List<RequirementResponse> findAll() {
        return requirementRepository.findAllByOrderByNameAsc().stream()
                .map(RequirementResponse::from)
                .toList();
    }

    public PhotoRequirement getOrThrow(Long id) {
        return requirementRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("요구 사진을 찾을 수 없습니다."));
    }

    @Transactional
    public RequirementResponse create(String name, String description, String classificationHint,
                                      MultipartFile exampleImage) {
        if (requirementRepository.existsByName(name)) {
            throw BusinessException.conflict("이미 존재하는 사진 명칭입니다: " + name);
        }
        String exampleFileName = null;
        if (exampleImage != null && !exampleImage.isEmpty()) {
            exampleFileName = fileStorageService.store(exampleImage).storedFileName();
        }
        PhotoRequirement saved = requirementRepository.save(
                new PhotoRequirement(name, description, classificationHint, exampleFileName));
        return RequirementResponse.from(saved);
    }

    @Transactional
    public RequirementResponse update(Long id, String name, String description, String classificationHint,
                                      MultipartFile exampleImage) {
        PhotoRequirement req = getOrThrow(id);
        if (!req.getName().equals(name) && requirementRepository.existsByName(name)) {
            throw BusinessException.conflict("이미 존재하는 사진 명칭입니다: " + name);
        }
        req.update(name, description, classificationHint);
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
        PhotoRequirement req = getOrThrow(id);
        if (submissionItemRepository.existsByRequirementId(id)) {
            throw BusinessException.conflict("이미 기업에 지정된 요구 사진은 삭제할 수 없습니다.");
        }
        fileStorageService.delete(req.getExampleImageFileName());
        requirementRepository.delete(req);
    }
}
