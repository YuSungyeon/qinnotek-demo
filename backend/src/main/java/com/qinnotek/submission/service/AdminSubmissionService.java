package com.qinnotek.submission.service;

import com.qinnotek.submission.domain.Company;
import com.qinnotek.submission.domain.CompanyStatus;
import com.qinnotek.submission.domain.SubmissionItem;
import com.qinnotek.submission.dto.admin.CompanySubmissionResponse;
import com.qinnotek.submission.dto.admin.SubmissionItemResponse;
import com.qinnotek.submission.exception.BusinessException;
import com.qinnotek.submission.repository.CompanyRepository;
import com.qinnotek.submission.repository.SubmissionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 관리자 제출 자료 조회 및 검수(통과/반환).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSubmissionService {

    private final CompanyRepository companyRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final FileStorageService fileStorageService;

    public CompanySubmissionResponse getCompanySubmissions(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> BusinessException.notFound("기업을 찾을 수 없습니다."));
        List<SubmissionItem> items = submissionItemRepository.findByCompanyIdWithRequirement(companyId);

        List<SubmissionItemResponse> itemResponses = items.stream()
                .map(SubmissionItemResponse::from)
                .toList();

        LocalDateTime latest = items.stream()
                .map(SubmissionItem::getUploadedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        CompanyStatus status = CompanyStatus.from(items);
        return new CompanySubmissionResponse(
                company.getId(), company.getName(), company.getPhoneNumber(),
                status.name(), status.getLabel(),
                latest, itemResponses);
    }

    @Transactional
    public SubmissionItemResponse approve(Long itemId) {
        SubmissionItem item = getItemOrThrow(itemId);
        item.approve();
        return SubmissionItemResponse.from(item);
    }

    /**
     * 반환 - 기존 제출본(원본 + PDF 사본) 삭제 후 반환 상태/사유 저장. 고객은 다시 제출해야 한다.
     */
    @Transactional
    public SubmissionItemResponse markReturned(Long itemId, String reason) {
        SubmissionItem item = getItemOrThrow(itemId);
        String stored = item.getStoredFileName();
        String pdf = item.getPdfFileName();
        item.markReturned(reason);
        fileStorageService.delete(stored);
        // 원본이 PDF 였다면 같은 파일이라 한 번만 지운다
        if (pdf != null && !pdf.equals(stored)) {
            fileStorageService.delete(pdf);
        }
        return SubmissionItemResponse.from(item);
    }

    /** 관리자 다운로드용 - 원본 또는 PDF 사본. */
    public record DownloadTarget(String storedFileName, String downloadName) {
    }

    public DownloadTarget originalDownload(Long itemId) {
        SubmissionItem item = getItemOrThrow(itemId);
        if (!item.hasFile()) {
            throw BusinessException.notFound("제출된 파일이 없습니다.");
        }
        String name = item.getOriginalFileName() != null && !item.getOriginalFileName().isBlank()
                ? item.getOriginalFileName()
                : displayName(item) + extensionOf(item.getStoredFileName());
        return new DownloadTarget(item.getStoredFileName(), name);
    }

    public DownloadTarget pdfDownload(Long itemId) {
        SubmissionItem item = getItemOrThrow(itemId);
        if (!item.hasPdfCopy()) {
            throw BusinessException.notFound("PDF 사본이 없습니다.");
        }
        return new DownloadTarget(item.getPdfFileName(), displayName(item) + ".pdf");
    }

    private static String displayName(SubmissionItem item) {
        return item.getCompany().getName() + "_" + item.getRequirement().getName();
    }

    /** 기업별 추가 설명 저장 (빈 값이면 삭제) */
    @Transactional
    public SubmissionItemResponse updateNote(Long itemId, String note) {
        SubmissionItem item = getItemOrThrow(itemId);
        item.changeAdminNote(note);
        return SubmissionItemResponse.from(item);
    }

    private SubmissionItem getItemOrThrow(Long itemId) {
        return submissionItemRepository.findById(itemId)
                .orElseThrow(() -> BusinessException.notFound("제출 항목을 찾을 수 없습니다."));
    }

    /** ZIP 에 담을 범위. */
    public enum ZipScope {
        /** 원본 + PDF 사본 */
        BOTH,
        /** PDF 사본만 (없으면 원본) */
        PDF,
        /** 원본만 */
        ORIGINAL;

        public static ZipScope from(String value) {
            if (value == null || value.isBlank()) {
                return BOTH;
            }
            try {
                return valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return BOTH;
            }
        }
    }

    /** ZIP 준비 결과 - 다운로드 파일명과 (엔트리명, 저장 파일명) 목록. */
    public record ZipPlan(String fileName, List<ZipEntrySpec> entries) {
    }

    public record ZipEntrySpec(String entryName, String storedFileName) {
    }

    /**
     * 통과(APPROVED)된 제출본을 ZIP 으로 묶을 계획을 만든다.
     *
     * <p>실제 스트리밍은 컨트롤러에서 한다. open-in-view=false 라 응답 스트림 안에서는
     * 지연 로딩이 불가능하므로, 트랜잭션 안에서 필요한 값만 미리 평탄화해 넘긴다.
     */
    public ZipPlan planApprovedZip(Long companyId, ZipScope scope) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> BusinessException.notFound("기업을 찾을 수 없습니다."));
        List<SubmissionItem> approved = submissionItemRepository.findByCompanyIdWithRequirement(companyId).stream()
                .filter(i -> i.getStatus() == com.qinnotek.submission.domain.SubmissionStatus.APPROVED && i.hasFile())
                .toList();
        if (approved.isEmpty()) {
            throw BusinessException.badRequest("다운로드할 완료(통과) 항목이 없습니다.");
        }

        java.util.Set<String> usedNames = new java.util.HashSet<>();
        List<ZipEntrySpec> entries = new java.util.ArrayList<>();
        for (SubmissionItem item : approved) {
            String base = sanitize(company.getName() + "_" + item.getRequirement().getName());
            boolean pdfIsSeparate = item.hasPdfCopy() && !item.getPdfFileName().equals(item.getStoredFileName());

            boolean wantOriginal = scope != ZipScope.PDF || !item.hasPdfCopy();
            boolean wantPdf = scope != ZipScope.ORIGINAL && item.hasPdfCopy();

            if (wantOriginal) {
                entries.add(new ZipEntrySpec(
                        uniquify(base + extensionOf(item.getStoredFileName()), usedNames),
                        item.getStoredFileName()));
            }
            // 원본이 이미 PDF 면 같은 파일을 두 번 담지 않는다
            if (wantPdf && (pdfIsSeparate || !wantOriginal)) {
                entries.add(new ZipEntrySpec(
                        uniquify(base + ".pdf", usedNames),
                        item.getPdfFileName()));
            }
        }
        return new ZipPlan(company.getName() + "_완료본.zip", entries);
    }

    /** ZIP 엔트리 하나를 스트림으로 흘려보낸다. */
    public void writeZipEntry(java.util.zip.ZipOutputStream zos, ZipEntrySpec entry) throws java.io.IOException {
        var resource = fileStorageService.loadAsResource(entry.storedFileName());
        zos.putNextEntry(new java.util.zip.ZipEntry(entry.entryName()));
        try (var in = resource.getInputStream()) {
            in.transferTo(zos);
        }
        zos.closeEntry();
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }

    private String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String uniquify(String name, java.util.Set<String> used) {
        String candidate = name;
        int i = 1;
        int dot = name.lastIndexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        String ext = dot >= 0 ? name.substring(dot) : "";
        while (!used.add(candidate)) {
            candidate = base + "_" + (++i) + ext;
        }
        return candidate;
    }
}
