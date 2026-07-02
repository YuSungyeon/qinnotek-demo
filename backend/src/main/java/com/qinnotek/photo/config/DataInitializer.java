package com.qinnotek.photo.config;

import com.qinnotek.photo.domain.Company;
import com.qinnotek.photo.domain.PhotoRequirement;
import com.qinnotek.photo.domain.SubmissionItem;
import com.qinnotek.photo.repository.CompanyRepository;
import com.qinnotek.photo.repository.PhotoRequirementRepository;
import com.qinnotek.photo.repository.SubmissionItemRepository;
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

    @Override
    public void run(String... args) {
        if (requirementRepository.count() > 0) {
            return;
        }
        log.info("데모용 초기 데이터 생성");

        PhotoRequirement front = requirementRepository.save(
                new PhotoRequirement("건물 정면", "건물 전체가 나오도록 정면에서 촬영해주세요.", null));
        PhotoRequirement signboard = requirementRepository.save(
                new PhotoRequirement("간판", "간판 상호가 선명하게 보이도록 촬영해주세요.", null));
        PhotoRequirement interior = requirementRepository.save(
                new PhotoRequirement("내부 전경", "매장 내부가 잘 보이도록 촬영해주세요.", null));
        requirementRepository.save(
                new PhotoRequirement("사업자등록증", "사업자등록증 전체가 나오도록 촬영해주세요.", null));

        Company a = new Company("가나상사");
        a.changePhoneNumber("01012345678");
        a = companyRepository.save(a);
        Company b = new Company("다라마트");
        b.changePhoneNumber("01055556666");
        b = companyRepository.save(b);
        companyRepository.save(new Company("전화번호미등록업체"));

        // 가나상사: 3개 항목 지정(제출 대기)
        submissionItemRepository.saveAll(List.of(
                new SubmissionItem(a, front),
                new SubmissionItem(a, signboard),
                new SubmissionItem(a, interior)
        ));
        // 다라마트: 2개 항목 지정
        submissionItemRepository.saveAll(List.of(
                new SubmissionItem(b, front),
                new SubmissionItem(b, signboard)
        ));

        log.info("초기 데이터 생성 완료 - 조회 테스트: 01012345678 / 01055556666");
    }
}
