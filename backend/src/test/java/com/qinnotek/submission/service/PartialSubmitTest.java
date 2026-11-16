package com.qinnotek.submission.service;

import com.qinnotek.submission.domain.Company;
import com.qinnotek.submission.domain.Requirement;
import com.qinnotek.submission.domain.SubmissionItem;
import com.qinnotek.submission.domain.SubmissionStatus;
import com.qinnotek.submission.dto.customer.CustomerItemResponse;
import com.qinnotek.submission.dto.customer.CustomerStatusResponse;
import com.qinnotek.submission.exception.BusinessException;
import com.qinnotek.submission.repository.CompanyRepository;
import com.qinnotek.submission.repository.RequirementRepository;
import com.qinnotek.submission.repository.SubmissionItemRepository;
import com.qinnotek.submission.util.LinkTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 부분 제출: 일부만 먼저 내도 남은 항목은 같은 링크로 이어서 낼 수 있어야 한다.
 */
@SpringBootTest
class PartialSubmitTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired RequirementRepository requirementRepository;
    @Autowired SubmissionItemRepository submissionItemRepository;
    @Autowired CustomerService customerService;
    @Autowired AdminSubmissionService adminSubmissionService;

    @Test
    void 일부만_먼저_내고_검수를_받는_동안에도_남은_항목을_이어서_낸다() {
        String token = LinkTokens.generate();
        Company company = new Company("부분제출테스트");
        company.issueLink(token, null);
        company = companyRepository.save(company);

        SubmissionItem a = item(company, "부분제출 A");
        SubmissionItem b = withFile(item(company, "부분제출 B"));
        SubmissionItem c = withFile(item(company, "부분제출 C"));

        // B·C 만 먼저 보낸다 → 화면이 잠기지 않고 A 만 남는다
        CustomerStatusResponse sent = customerService.submit(token, List.of(b.getId(), c.getId()));
        assertThat(sent.state()).isEqualTo("INITIAL");
        assertThat(sent.items()).extracting(CustomerItemResponse::itemId).containsExactly(a.getId());
        assertThat(sent.reviewingNames()).containsExactly("부분제출 B", "부분제출 C");
        assertThat(statusOf(b)).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(statusOf(a)).isEqualTo(SubmissionStatus.PENDING);

        // 파일이 없는 항목은 낼 수 없다
        assertThatThrownBy(() -> customerService.submit(token, List.of(a.getId())))
                .isInstanceOf(BusinessException.class);

        // 담당자가 B 통과, C 반환 → 다시 열면 반환된 C 가 먼저, 이어서 아직 안 낸 A
        adminSubmissionService.approve(b.getId());
        adminSubmissionService.markReturned(c.getId(), "다시 촬영해 주세요", true);
        CustomerStatusResponse reopened = customerService.open(token);
        assertThat(reopened.state()).isEqualTo("INITIAL");
        assertThat(reopened.items()).extracting(CustomerItemResponse::itemId).containsExactly(c.getId(), a.getId());
        assertThat(reopened.approvedCount()).isEqualTo(1);
        assertThat(reopened.reviewingNames()).isEmpty();
    }

    private SubmissionItem item(Company company, String name) {
        Requirement req = requirementRepository.save(new Requirement(name, "설명", null, null));
        return submissionItemRepository.save(new SubmissionItem(company, req));
    }

    private SubmissionItem withFile(SubmissionItem item) {
        item.attachPhoto(item.getId() + ".jpg", "photo.jpg", "/tmp/" + item.getId() + ".jpg");
        return submissionItemRepository.save(item);
    }

    private SubmissionStatus statusOf(SubmissionItem item) {
        return submissionItemRepository.findById(item.getId()).orElseThrow().getStatus();
    }
}
