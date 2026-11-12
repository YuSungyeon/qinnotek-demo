package com.qinnotek.submission.dto.admin;

import com.qinnotek.submission.domain.Requirement;

/**
 * 요청 자료(마스터) 응답.
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
    public static RequirementResponse from(Requirement req) {
        String url = req.getExampleImageFileName() != null
                ? "/api/files/" + req.getExampleImageFileName() : null;
        return new RequirementResponse(
                req.getId(), req.getName(), req.getDescription(), req.getClassificationHint(), url,
                req.getFileType().name(), req.getFileType().getLabel(), req.getDocumentKeywords());
    }
}
