package com.qinnotek.photo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 고객이 올린 문서의 임시 보관본. 서버가 PDF 변환과 텍스트 추출을 끝낸 뒤
 * 자동 분류 결과를 고객에게 보여주고, 고객이 확정하면 SubmissionItem 으로 소유권이 넘어간다.
 *
 * <p>확정되지 않은 채 남은 행은 정리 배치가 파일과 함께 지운다.
 */
@Entity
@Table(name = "staged_document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StagedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    /** 원본 저장 파일명(UUID) */
    @Column(nullable = false)
    private String storedFileName;

    /** 고객이 올린 원래 파일명 */
    private String originalFileName;

    /** 원본 저장 경로 */
    private String storagePath;

    private long sizeBytes;

    /** PDF 사본 파일명. 원본이 PDF 면 storedFileName 과 같다 */
    private String pdfFileName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConversionStatus conversionStatus = ConversionStatus.PENDING;

    /** 변환 실패 사유 (고객에게 그대로 노출) */
    @Column(length = 500)
    private String failureMessage;

    /** 분류용 추출 텍스트. MySQL 에서 TEXT 로 매핑된다 (DocumentTextExtractor 가 12,000자로 자름) */
    @Column(length = 20000)
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TextStatus textStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StagedState state = StagedState.STAGED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public StagedDocument(Company company, String storedFileName, String originalFileName,
                          String storagePath, long sizeBytes) {
        this.company = company;
        this.storedFileName = storedFileName;
        this.originalFileName = originalFileName;
        this.storagePath = storagePath;
        this.sizeBytes = sizeBytes;
        this.conversionStatus = ConversionStatus.PENDING;
        this.state = StagedState.STAGED;
        this.createdAt = LocalDateTime.now();
    }

    /** 변환 성공 - PDF 사본과 추출 텍스트 기록 */
    public void markProcessed(String pdfFileName, String extractedText, TextStatus textStatus) {
        this.pdfFileName = pdfFileName;
        this.conversionStatus = ConversionStatus.READY;
        this.failureMessage = null;
        this.extractedText = extractedText;
        this.textStatus = textStatus;
    }

    /** 변환 실패 - 원본만 보관한다. 분류는 파일명으로만 시도한다 */
    public void markFailed(String message) {
        this.conversionStatus = ConversionStatus.FAILED;
        this.failureMessage = message;
        this.pdfFileName = null;
        this.extractedText = null;
        this.textStatus = TextStatus.ERROR;
    }

    /** 제출 항목에 확정 - 파일 소유권이 넘어가므로 여기서 파일을 지우면 안 된다 */
    public void consume() {
        this.state = StagedState.CONSUMED;
    }

    public boolean isProcessing() {
        return conversionStatus == ConversionStatus.PENDING;
    }
}
