package com.qinnotek.submission.config;

import com.qinnotek.submission.domain.Company;
import com.qinnotek.submission.domain.CompanyManager;
import com.qinnotek.submission.domain.Manager;
import com.qinnotek.submission.domain.Requirement;
import com.qinnotek.submission.domain.SubmissionItem;
import com.qinnotek.submission.repository.CompanyManagerRepository;
import com.qinnotek.submission.repository.CompanyRepository;
import com.qinnotek.submission.repository.ManagerRepository;
import com.qinnotek.submission.repository.RequirementRepository;
import com.qinnotek.submission.repository.SubmissionItemRepository;
import com.qinnotek.submission.service.RequirementSeeder;
import com.qinnotek.submission.util.LinkTokens;
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
    private final RequirementRepository requirementRepository;
    private final SubmissionItemRepository submissionItemRepository;
    private final ManagerRepository managerRepository;
    private final CompanyManagerRepository companyManagerRepository;
    private final RequirementSeeder requirementSeeder;

    @Override
    public void run(String... args) {
        if (requirementRepository.count() > 0) {
            return;
        }
        log.info("데모용 초기 데이터 생성");

        // 요청 자료 마스터: 제조 공장 실사 카탈로그 (운영에 넣은 것과 같은 목록)
        requirementSeeder.seed();
        Requirement exterior = req("공장 외관");
        Requirement line = req("생산 라인");
        Requirement nameplate = req("설비 명판");
        Requirement warehouse = req("자재 창고");
        Requirement finished = req("완제품 적재");
        Requirement inspection = req("품질 검사 장비");
        Requirement fireSafety = req("소방 설비");
        Requirement bizLicense = req("사업자등록증");
        Requirement factoryReg = req("공장등록증명서");
        Requirement iso = req("ISO 인증서");
        Requirement equipmentList = req("설비 목록");
        Requirement processFlow = req("공정 흐름도");

        // ---- 기업 ----
        Company a = new Company("한빛정밀(주)");
        a.changePhoneNumber("01012345678");
        a.issueLink(LinkTokens.generate(), null);
        a = companyRepository.save(a);
        Company b = new Company("대성금속(주)");
        b.changePhoneNumber("01055556666");
        b.issueLink(LinkTokens.generate(), null);
        b = companyRepository.save(b);
        companyRepository.save(new Company("미래플라스틱")); // 요청 전 - 링크·항목 없음

        // 한빛정밀: 신규 거래 실사 - 현장 사진 4 + 증명서 2 + 파일 1 (사진/문서 혼합 시나리오)
        submissionItemRepository.saveAll(List.of(
                new SubmissionItem(a, exterior),
                new SubmissionItem(a, line),
                new SubmissionItem(a, nameplate),
                new SubmissionItem(a, warehouse),
                new SubmissionItem(a, bizLicense),
                new SubmissionItem(a, factoryReg),
                new SubmissionItem(a, equipmentList)
        ));
        // 대성금속: 품질 점검 - 사진 3 + 인증서 1 + 파일 1
        submissionItemRepository.saveAll(List.of(
                new SubmissionItem(b, inspection),
                new SubmissionItem(b, finished),
                new SubmissionItem(b, fireSafety),
                new SubmissionItem(b, iso),
                new SubmissionItem(b, processFlow)
        ));
        // safetySign, production 은 지정하지 않은 채로 둔다 (관리자 화면에서 추가 지정 테스트용)

        // 담당자 마스터 + 기업별 지정
        Manager m1 = managerRepository.save(new Manager("김현우", "구매팀장", "01011112222"));
        Manager m2 = managerRepository.save(new Manager("이서연", "품질팀장", "01033334444"));
        managerRepository.save(new Manager("박준호", "생산관리 대리", "01055557777"));
        companyManagerRepository.saveAll(List.of(
                new CompanyManager(a, m1),
                new CompanyManager(a, m2),
                new CompanyManager(b, m2)
        ));

        log.info("초기 데이터 생성 완료 - 요청 자료 {}개, 제출 링크: {} → http://localhost:5173/s/{}, {} → http://localhost:5173/s/{}",
                requirementRepository.count(),
                a.getName(), a.getAccessToken(), b.getName(), b.getAccessToken());
    }

    private Requirement req(String name) {
        return requirementRepository.findByName(name).orElseThrow();
    }
}
