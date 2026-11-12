package com.qinnotek.photo.repository;

import com.qinnotek.photo.domain.StagedDocument;
import com.qinnotek.photo.domain.StagedState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StagedDocumentRepository extends JpaRepository<StagedDocument, Long> {

    List<StagedDocument> findByCompanyIdAndStateOrderByIdAsc(Long companyId, StagedState state);

    List<StagedDocument> findByCreatedAtBefore(LocalDateTime cutoff);
}
