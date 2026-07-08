package com.qinnotek.photo.service;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.CompanyStatus;
import com.qinnotek.photo.domain.SubmissionItem;
import com.qinnotek.photo.dto.admin.CompanySubmissionResponse;
import com.qinnotek.photo.dto.admin.SubmissionItemResponse;
import com.qinnotek.photo.exception.BusinessException;
import com.qinnotek.photo.repository.CompanyRepository;
import com.qinnotek.photo.repository.SubmissionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 관리자 제출 사진 조회 및 검수(통과/반환).
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
                status.name(), status.getLabel(), status.getColor(),
                latest, itemResponses);
    }

    @Transactional
    public SubmissionItemResponse approve(Long itemId) {
        SubmissionItem item = getItemOrThrow(itemId);
        item.approve();
        return SubmissionItemResponse.from(item);
    }

    /**
     * 반환 - 기존 사진 삭제 후 반환 상태/사유 저장. 고객은 재촬영 필요.
     */
    @Transactional
    public SubmissionItemResponse markReturned(Long itemId, String reason) {
        SubmissionItem item = getItemOrThrow(itemId);
        String stored = item.getStoredFileName();
        item.markReturned(reason);
        fileStorageService.delete(stored);
        return SubmissionItemResponse.from(item);
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

    /** ZIP 다운로드 결과 (파일명 + 바이트) */
    public record ZipResult(String fileName, byte[] data) {
    }

    /**
     * 통과(APPROVED)된 사진들을 하나의 ZIP으로 묶는다. 파일명은 기업명_사진명 형태.
     */
    public ZipResult zipApprovedPhotos(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> BusinessException.notFound("기업을 찾을 수 없습니다."));
        List<SubmissionItem> approved = submissionItemRepository.findByCompanyIdWithRequirement(companyId).stream()
                .filter(i -> i.getStatus() == com.qinnotek.photo.domain.SubmissionStatus.APPROVED && i.hasPhoto())
                .toList();
        if (approved.isEmpty()) {
            throw BusinessException.badRequest("다운로드할 완료(통과) 사진이 없습니다.");
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.util.Set<String> usedNames = new java.util.HashSet<>();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            for (SubmissionItem item : approved) {
                var resource = fileStorageService.loadAsResource(item.getStoredFileName());
                String ext = extensionOf(item.getStoredFileName());
                String base = sanitize(company.getName() + "_" + item.getRequirement().getName());
                String entryName = uniquify(base + ext, usedNames);
                zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                try (var in = resource.getInputStream()) {
                    in.transferTo(zos);
                }
                zos.closeEntry();
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("ZIP 생성 실패", e);
        }
        return new ZipResult(company.getName() + "_완료사진.zip", baos.toByteArray());
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
