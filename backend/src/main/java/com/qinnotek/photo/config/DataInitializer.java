package com.qinnotek.photo.config;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.CompanyManager;
import com.qinnotek.photo.domain.Manager;
import com.qinnotek.photo.domain.PhotoRequirement;
import com.qinnotek.photo.domain.RequirementFileType;
import com.qinnotek.photo.domain.SubmissionItem;
import com.qinnotek.photo.repository.CompanyManagerRepository;
import com.qinnotek.photo.repository.CompanyRepository;
import com.qinnotek.photo.repository.ManagerRepository;
import com.qinnotek.photo.repository.PhotoRequirementRepository;
import com.qinnotek.photo.repository.SubmissionItemRepository;
import com.qinnotek.photo.util.LinkTokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 데모용 초기 데이터. prod 프로필이 아닐 때만 동작(H2 인메모리).
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final PhotoRequirementRepository requirementRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final ManagerRepository managerRepository;
    private final CompanyManagerRepository companyManagerRepository;

    @Override
    public void run(String... args) {
        if (requirementRepository.count() > 0) {
            return;
        }
        log.info("데모용 초기 데이터 생성");

        PhotoRequirement front = requirementRepository.save(
                new PhotoRequirement("건물 정면", "건물 전체가 나오도록 정면에서 촬영해주세요.",
                        "the exterior facade of a building seen from outside", null));
        PhotoRequirement signboard = requirementRepository.save(
                new PhotoRequirement("간판", "간판 상호가 선명하게 보이도록 촬영해주세요.",
                        "a storefront signboard with shop name text", null));
        PhotoRequirement interior = requirementRepository.save(
                new PhotoRequirement("내부 전경", "매장 내부가 잘 보이도록 촬영해주세요.",
                        "the indoor interior of a store or room", null));
        // 문서 항목 - 사진 촬영본도 받을 수 있도록 ANY, 서버 텍스트 분류용 키워드 지정
        PhotoRequirement license = requirementRepository.save(
                new PhotoRequirement("사업자등록증", "사업자등록증 사본(파일) 또는 전체가 나오도록 촬영한 사진을 올려주세요.",
                        "a paper document certificate with printed text", null,
                        RequirementFileType.ANY, "사업자등록증,등록번호,대표자,개업연월일,사업장소재지"));
        PhotoRequirement lease = requirementRepository.save(
                new PhotoRequirement("임대차계약서", "임대차계약서 파일을 올려주세요. (워드/한글/PDF 가능)",
                        null, null,
                        RequirementFileType.DOCUMENT, "임대차계약서,임대인,임차인,보증금,월세,계약기간"));

        Company a = new Company("가나상사");
        a.changePhoneNumber("01012345678");
        a.issueLink(LinkTokens.generate(), null);
        a = companyRepository.save(a);
        Company b = new Company("다라마트");
        b.changePhoneNumber("01055556666");
        b.issueLink(LinkTokens.generate(), null);
        b = companyRepository.save(b);
        companyRepository.save(new Company("전화번호미등록업체"));

        // 가나상사: 사진 3 + 문서 2 (사진/문서 혼합 제출 시나리오)
        submissionItemRepository.saveAll(List.of(
                new SubmissionItem(a, front),
                new SubmissionItem(a, signboard),
                new SubmissionItem(a, interior),
                new SubmissionItem(a, license),
                new SubmissionItem(a, lease)
        ));
        // 다라마트: 2개 항목 지정
        submissionItemRepository.saveAll(List.of(
                new SubmissionItem(b, front),
                new SubmissionItem(b, signboard)
        ));

        // 담당자 마스터 + 기업별 지정
        Manager m1 = managerRepository.save(new Manager("김점장", "점장", "01011112222"));
        Manager m2 = managerRepository.save(new Manager("이사장", "대표", "01033334444"));
        managerRepository.save(new Manager("박대리", "대리", "01055557777"));
        companyManagerRepository.saveAll(List.of(
                new CompanyManager(a, m1),
                new CompanyManager(a, m2)
        ));

        log.info("초기 데이터 생성 완료 - 제출 링크: {} → http://localhost:5173/s/{}, {} → http://localhost:5173/s/{}",
                a.getName(), a.getAccessToken(), b.getName(), b.getAccessToken());
    }
}
