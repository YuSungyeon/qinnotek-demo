package com.qinnotek.submission.dto.customer;

import jakarta.validation.constraints.NotNull;

/** 스테이징된 문서를 제출 항목에 확정한다. */
public record AttachDocumentRequest(@NotNull Long stagedId) {
}
