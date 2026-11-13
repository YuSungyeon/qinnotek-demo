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
        String documentKeywords,
        /** 이 요청 자료가 지정된 기업 수 (삭제 가능 여부 판단·목록 표시용) */
        long assignedCount
) {
    public static RequirementResponse from(Requirement req) {
        return from(req, 0);
    }

    public static RequirementResponse from(Requirement req, long assignedCount) {
        String url = req.getExampleImageFileName() != null
                ? "/api/files/" + req.getExampleImageFileName() : null;
        return new RequirementResponse(
                req.getId(), req.getName(), req.getDescription(), req.getClassificationHint(), url,
                req.getFileType().name(), req.getFileType().getLabel(), req.getDocumentKeywords(), assignedCount);
    }
}
