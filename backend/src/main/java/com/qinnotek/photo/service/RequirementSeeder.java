package com.qinnotek.photo.service;

import com.qinnotek.photo.domain.PhotoRequirement;
import com.qinnotek.photo.domain.RequirementFileType;
import com.qinnotek.photo.repository.PhotoRequirementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 요구 사진 예시 카탈로그(제조 공장 실사 시나리오).
 * 로컬 초기 데이터(DataInitializer)가 쓴다. 운영 DB 에는 2026-11-12 에 한 번 반영했다.
 *
 * <p>같은 이름의 항목이 이미 있으면 건드리지 않고 건너뛴다 - 여러 번 실행해도 안전하다.
 * 예시 이미지는 jar 안의 seed/requirements/*.jpg 를 업로드 저장소로 복사해 붙인다 (출처: ATTRIBUTION.md).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementSeeder {

    private static final String IMAGE_DIR = "seed/requirements/";

    /**
     * @param hint     기기 내 CLIP 분류용 영문 설명 - 항목끼리 겹치지 않게 피사체를 구체적으로
     * @param keywords 서버 문서 분류용 키워드 - 그 서류에만 나오는 단어 위주
     * @param image    예시 이미지 파일명(seed/requirements/ 아래). 없으면 null
     */
    public record Entry(String name, String description, RequirementFileType fileType,
                        String hint, String keywords, String image) {
    }

    public record Result(List<String> created, List<String> skipped) {
    }

    public static final List<Entry> CATALOG = List.of(
            // 사진: 현장
            photo("공장 외관", "공장 건물 전체와 출입구가 함께 나오도록 밖에서 촬영해주세요.",
                    "the outside of a factory building with a large loading gate", "factory-exterior.jpg"),
            photo("생산 라인", "가동 중인 생산 라인 전체가 한 화면에 들어오도록 촬영해주세요.",
                    "an indoor factory production line with machines and a conveyor belt", "production-line.jpg"),
            photo("설비 명판", "주요 설비의 명판(모델명·제조번호)이 읽히도록 가까이서 촬영해주세요.",
                    "a close-up of a metal nameplate on a machine with model and serial number text",
                    "equipment-nameplate.jpg"),
            photo("자재 창고", "원자재가 보관된 창고의 랙과 적재 상태가 보이도록 촬영해주세요.",
                    "a warehouse with tall storage racks and stacked pallets of raw materials",
                    "material-warehouse.jpg"),
            photo("완제품 적재", "출하 대기 중인 완제품의 포장·라벨 상태가 보이도록 촬영해주세요.",
                    "boxed finished products wrapped in plastic on a pallet ready for shipping",
                    "finished-goods-pallet.jpg"),
            photo("품질 검사 장비", "검사실 또는 검사대의 측정 장비가 보이도록 촬영해주세요.",
                    "precision measuring instruments and calipers on a quality inspection table",
                    "quality-inspection.jpg"),
            photo("소방 설비", "소화기와 소화전 위치가 함께 보이도록 촬영해주세요.",
                    "a red fire extinguisher and a fire hose cabinet mounted on a wall", "fire-safety.jpg"),
            photo("안전 표지 게시", "작업장 안전 수칙·경고 표지가 게시된 모습을 촬영해주세요.",
                    "yellow and black safety warning signs posted on a factory wall", "safety-signs.jpg"),

            // 사진 또는 파일: 증명서류 (실제 업체 정보가 담긴 서류라 예시 이미지는 두지 않는다)
            new Entry("사업자등록증", "사업자등록증 사본 파일 또는 전체가 나오도록 촬영한 사진을 올려주세요.",
                    RequirementFileType.ANY,
                    "a Korean business registration certificate paper with a red official stamp",
                    "사업자등록증,등록번호,법인등록번호,대표자,개업연월일,사업장소재지,업태,종목", null),
            new Entry("공장등록증명서", "공장등록증명서(최근 발급본) 파일 또는 촬영 사진을 올려주세요.",
                    RequirementFileType.ANY,
                    "an official government form with a ruled table of factory details",
                    "공장등록증명서,공장등록,공장소재지,업종,생산품,제조시설,부지면적,등록일", null),
            new Entry("ISO 인증서", "ISO 9001 등 보유한 품질·환경 인증서를 올려주세요.",
                    RequirementFileType.ANY,
                    "an ISO quality certificate with a certification body logo and decorative border",
                    "ISO,9001,14001,인증서,Certificate,품질경영시스템,인증범위,유효기간", null),

            // 파일: 내부 자료
            doc("설비 목록", "보유 설비 목록을 올려주세요. (엑셀/한글/PDF 가능)",
                    "설비목록,설비명,모델명,제조사,제조번호,수량,도입연도,설비현황"),
            doc("공정 흐름도", "입고부터 출하까지의 공정 흐름도를 올려주세요.",
                    "공정흐름도,공정도,입고,가공,조립,검사,포장,출하"),
            doc("최근 3개월 생산 실적", "최근 3개월 월별 생산 실적 자료를 올려주세요. (엑셀 권장)",
                    "생산실적,생산량,계획대비,월별,품목,가동률,불량률")
    );

    private final PhotoRequirementRepository requirementRepository;
    private final FileStorageService fileStorageService;

    /** 카탈로그 중 아직 없는 이름만 추가한다. */
    @Transactional
    public Result seed() {
        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Entry e : CATALOG) {
            if (requirementRepository.existsByName(e.name())) {
                skipped.add(e.name());
                continue;
            }
            requirementRepository.save(new PhotoRequirement(e.name(), e.description(), e.hint(),
                    storeImage(e.image()), e.fileType(), e.keywords()));
            created.add(e.name());
        }
        log.info("[요구 사진 예시] 추가 {}개, 이미 있어 건너뜀 {}개", created.size(), skipped.size());
        return new Result(created, skipped);
    }

    /** jar 안의 예시 이미지를 업로드 저장소로 복사한다. 이미지가 없거나 실패하면 null (항목은 그대로 추가). */
    private String storeImage(String image) {
        if (image == null) {
            return null;
        }
        ClassPathResource resource = new ClassPathResource(IMAGE_DIR + image);
        if (!resource.exists()) {
            log.warn("[요구 사진 예시] 이미지 없음: {}", image);
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            Path tmp = Files.createTempFile(fileStorageService.getTmpRoot(), "seed-", ".jpg");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            return fileStorageService.storeFromPath(tmp, image, "jpg").storedFileName();
        } catch (IOException | RuntimeException ex) {
            log.warn("[요구 사진 예시] 이미지 복사 실패 {}: {}", image, ex.getMessage());
            return null;
        }
    }

    private static Entry photo(String name, String description, String hint, String image) {
        return new Entry(name, description, RequirementFileType.PHOTO, hint, null, image);
    }

    private static Entry doc(String name, String description, String keywords) {
        return new Entry(name, description, RequirementFileType.DOCUMENT, null, keywords, null);
    }
}
