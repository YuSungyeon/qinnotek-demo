package com.qinnotek.submission.service;

import com.qinnotek.submission.domain.Company;
import com.qinnotek.submission.domain.Requirement;
import com.qinnotek.submission.domain.SubmissionItem;
import com.qinnotek.submission.dto.admin.SubmissionEventResponse;
import com.qinnotek.submission.repository.CompanyRepository;
import com.qinnotek.submission.repository.RequirementRepository;
import com.qinnotek.submission.repository.SubmissionItemRepository;
import com.qinnotek.submission.util.LinkTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 처리 이력 + 반환 안내.
 * - 반환할 때마다 보내지 않고, 그 기업의 검수를 마칠 때 반환 항목을 모아 한 번 보낸다.
 * - 반환한 파일은 지우지 않고 이력이 보관한다.
 */
@SpringBootTest
class ReviewHistoryTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired RequirementRepository requirementRepository;
    @Autowired SubmissionItemRepository submissionItemRepository;
    @Autowired CustomerService customerService;
    @Autowired AdminSubmissionService adminSubmissionService;
    @Autowired SubmissionHistoryService historyService;

    @MockBean SmsService smsService;

    @Test
    void 반환은_모았다가_검수를_마칠_때_한_번만_안내하고_반환한_파일은_이력에_남는다() {
        String token = LinkTokens.generate();
        Company company = company("이력테스트", token, "01077778888");
        SubmissionItem a = withFile(item(company, "이력 A"), "a1.jpg");
        SubmissionItem b = withFile(item(company, "이력 B"), "b1.jpg");
        SubmissionItem c = withFile(item(company, "이력 C"), "c1.jpg");
        customerService.submit(token, List.of(a.getId(), b.getId(), c.getId()));

        // A 반환, B 통과 - 아직 C 가 검수 대기라 안내하지 않는다
        assertThat(adminSubmissionService.markReturned(a.getId(), "너무 어두워요", true).notice()).isNull();
        assertThat(adminSubmissionService.approve(b.getId()).notice()).isNull();
        verify(smsService, never()).sendReturnNotice(any(), any(), any(), any());

        // 마지막 C 를 통과시키면 검수가 끝나 A 반환 안내가 한 통 나간다
        var notice = adminSubmissionService.approve(c.getId()).notice();
        assertThat(notice.sent()).isTrue();
        assertThat(notice.count()).isEqualTo(1);
        verify(smsService, times(1)).sendReturnNotice(eq("01077778888"), eq("이력테스트"),
                argThat((List<SmsService.ReturnLine> lines) ->
                        lines.size() == 1 && lines.get(0).name().equals("이력 A")
                                && lines.get(0).reason().equals("너무 어두워요")),
                endsWith("/s/" + token));

        List<SubmissionEventResponse> history = historyService.list(company.getId());
        assertThat(history).extracting(SubmissionEventResponse::type).containsExactly(
                "NOTICE_SENT", "APPROVED", "APPROVED", "RETURNED", "SUBMITTED", "SUBMITTED", "SUBMITTED");
        // 반환한 파일은 지우지 않고 이력이 들고 있다
        SubmissionEventResponse returned = history.get(3);
        assertThat(returned.fileUrl()).isEqualTo("/api/files/a1.jpg");
        assertThat(returned.reason()).isEqualTo("너무 어두워요");

        // 고객이 A 를 다시 내면 2차 제출로 남는다
        withFile(submissionItemRepository.findById(a.getId()).orElseThrow(), "a2.jpg");
        customerService.submit(token, List.of(a.getId()));
        SubmissionEventResponse resubmitted = historyService.list(company.getId()).get(0);
        assertThat(resubmitted.type()).isEqualTo("SUBMITTED");
        assertThat(resubmitted.round()).isEqualTo(2);
    }

    @Test
    void 고객_연락처가_없으면_보내지_않고_실패로_남겨_다시_보내게_한다() {
        String token = LinkTokens.generate();
        Company company = company("연락처없음테스트", token, null);
        SubmissionItem a = withFile(item(company, "연락처없음 A"), "x1.jpg");
        customerService.submit(token, List.of(a.getId()));

        var notice = adminSubmissionService.markReturned(a.getId(), "다시 찍어 주세요", true).notice();
        assertThat(notice.sent()).isFalse();
        assertThat(notice.message()).isEqualTo("고객 연락처가 없어요.");
        verify(smsService, never()).sendReturnNotice(any(), any(), any(), any());

        // 화면에는 '보내지 못한 반환 안내'로 남는다
        var status = adminSubmissionService.getCompanySubmissions(company.getId()).returnNotice();
        assertThat(status.pending()).isTrue();
        assertThat(status.message()).isEqualTo("고객 연락처가 없어요.");
        assertThat(historyService.list(company.getId()).get(0).type()).isEqualTo("NOTICE_FAILED");
    }

    @Test
    void 알림을_끄고_반환하면_검수를_마쳐도_안내하지_않는다() {
        String token = LinkTokens.generate();
        Company company = company("알림끔테스트", token, "01099990000");
        SubmissionItem a = withFile(item(company, "알림끔 A"), "y1.jpg");
        customerService.submit(token, List.of(a.getId()));

        assertThat(adminSubmissionService.markReturned(a.getId(), "사유", false).notice()).isNull();
        verify(smsService, never()).sendReturnNotice(any(), any(), any(), any());
    }

    private Company company(String name, String token, String phone) {
        Company company = new Company(name);
        company.issueLink(token, null);
        company.changePhoneNumber(phone);
        return companyRepository.save(company);
    }

    private SubmissionItem item(Company company, String name) {
        Requirement req = requirementRepository.save(new Requirement(name, "설명", null, null));
        return submissionItemRepository.save(new SubmissionItem(company, req));
    }

    private SubmissionItem withFile(SubmissionItem item, String fileName) {
        item.attachPhoto(fileName, "photo.jpg", "/tmp/" + fileName);
        return submissionItemRepository.save(item);
    }
}
