package com.qinnotek.photo.dto.admin;

import com.qinnotek.photo.domain.PhotoRequirement;

/**
 * 요구 사진(마스터) 응답.
 */
public record RequirementResponse(
        Long id,
        String name,
        String description,
        String classificationHint,
        String exampleImageUrl,
        String fileType,
        String fileTypeLabel,
        String documentKeywords
) {
    public static RequirementResponse from(PhotoRequirement req) {
        String url = req.getExampleImageFileName() != null
                ? "/api/files/" + req.getExampleImageFileName() : null;
        return new RequirementResponse(
                req.getId(), req.getName(), req.getDescription(), req.getClassificationHint(), url,
                req.getFileType().name(), req.getFileType().getLabel(), req.getDocumentKeywords());
    }
}
