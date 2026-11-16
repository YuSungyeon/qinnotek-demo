package com.qinnotek.submission.repository;

import com.qinnotek.submission.domain.StagedDocument;
import com.qinnotek.submission.domain.StagedState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StagedDocumentRepository extends JpaRepository<StagedDocument, Long> {

    /**
     * 행 잠금 조회 (SELECT ... FOR UPDATE). 변환 결과 기록과 고객의 확정·취소가 서로의 변경을
     * 덮어쓰지 않도록 둘 다 이걸로 읽는다. 트랜잭션 안에서만 쓸 수 있다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StagedDocument s where s.id = :id")
    Optional<StagedDocument> findByIdForUpdate(@Param("id") Long id);

    List<StagedDocument> findByCompanyIdAndStateOrderByIdAsc(Long companyId, StagedState state);

    List<StagedDocument> findByCreatedAtBefore(LocalDateTime cutoff);
}
