package com.qinnotek.submission.service;

import com.qinnotek.submission.domain.*;
import com.qinnotek.submission.repository.CompanyRepository;
import com.qinnotek.submission.repository.RequirementRepository;
import com.qinnotek.submission.repository.StagedDocumentRepository;
import com.qinnotek.submission.repository.SubmissionItemRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 고객이 문서 변환(PDF 사본 만들기)이 끝나기 전에 전송해도 제출본이 온전해야 한다.
 * 예전에는 항목에 PDF 사본이 끝내 붙지 않았고, 변환 작업이 확정 상태를 덮어써 다음 날 청소 작업이 제출 문서를 지웠다.
 */
@SpringBootTest
class LateConversionTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired RequirementRepository requirementRepository;
    @Autowired SubmissionItemRepository submissionItemRepository;
    @Autowired StagedDocumentRepository stagedDocumentRepository;
    @Autowired StagedDocumentService stagedDocumentService;
    @Autowired DocumentProcessor documentProcessor;
    @Autowired FileStorageService fileStorageService;

    @Test
    void 변환_전에_확정한_문서도_변환이_끝나면_항목에_PDF가_붙고_확정_상태가_유지된다() throws IOException {
        Company company = companyRepository.save(new Company("변환지연테스트"));
        Requirement req = requirementRepository.save(new Requirement(
                "변환지연테스트 서류", "설명", null, null, RequirementFileType.DOCUMENT, null));
        SubmissionItem item = submissionItemRepository.save(new SubmissionItem(company, req));

        String stored = UUID.randomUUID().toString().replace("-", "") + ".pdf";
        Path file = fileStorageService.pathOf(stored);
        try (PDDocument pdf = new PDDocument()) {
            pdf.addPage(new PDPage());
            pdf.save(file.toFile());
        }
        StagedDocument staged = stagedDocumentRepository.save(new StagedDocument(
                company, stored, "사업자등록증.pdf", file.toString(), Files.size(file)));

        try {
            // 고객이 변환을 기다리지 않고 전송 → 아직 PENDING 인 문서가 항목에 확정된다
            stagedDocumentService.attach(company.getId(), item.getId(), staged.getId());
            assertThat(reload(item).getPdfStatus()).isEqualTo(ConversionStatus.PENDING);

            // 그 뒤에 변환이 끝난다
            documentProcessor.processNow(staged.getId());

            SubmissionItem after = reload(item);
            assertThat(after.getPdfStatus()).isEqualTo(ConversionStatus.READY);
            assertThat(after.getPdfFileName()).isEqualTo(stored); // 원본이 PDF 라 사본도 같은 파일
            assertThat(stagedDocumentRepository.findById(staged.getId()).orElseThrow().getState())
                    .isEqualTo(StagedState.CONSUMED); // STAGED 로 되돌아가면 청소 작업이 제출 문서를 지운다
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private SubmissionItem reload(SubmissionItem item) {
        return submissionItemRepository.findById(item.getId()).orElseThrow();
    }
}
