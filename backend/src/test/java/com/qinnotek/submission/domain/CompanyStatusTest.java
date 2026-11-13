package com.qinnotek.submission.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기업 종합 상태는 "지금 누가 움직여야 하는가"로 정한다.
 * 관리자 할 일(검수 대기) > 고객 할 일(재제출 대기) > 제출 대기 > 완료.
 */
class CompanyStatusTest {

    private final Company company = new Company("테스트기업");

    private SubmissionItem item(Consumer<SubmissionItem> state) {
        SubmissionItem i = new SubmissionItem(company, new Requirement("항목", null, null, null));
        state.accept(i);
        return i;
    }

    private final Consumer<SubmissionItem> pending = i -> { };
    private final Consumer<SubmissionItem> submitted = SubmissionItem::submit;
    private final Consumer<SubmissionItem> approved = SubmissionItem::approve;
    private final Consumer<SubmissionItem> returned = i -> i.markReturned("사유");

    @Test
    void 지정된_항목이_없으면_요청_없음() {
        assertThat(CompanyStatus.from(List.of())).isEqualTo(CompanyStatus.NONE);
    }

    @Test
    void 반환한_항목이_있어도_검수할_항목이_남아_있으면_검수_대기() {
        var items = List.of(item(submitted), item(returned), item(approved));
        assertThat(CompanyStatus.from(items)).isEqualTo(CompanyStatus.UNDER_REVIEW);
    }

    @Test
    void 검수할_것이_없고_반환한_항목이_있으면_재제출_대기() {
        var items = List.of(item(returned), item(approved));
        assertThat(CompanyStatus.from(items)).isEqualTo(CompanyStatus.PARTIALLY_RETURNED);
    }

    @Test
    void 아직_올리지_않은_항목만_있으면_제출_대기() {
        assertThat(CompanyStatus.from(List.of(item(pending), item(approved)))).isEqualTo(CompanyStatus.SUBMIT_WAITING);
    }

    @Test
    void 모두_통과하면_완료() {
        assertThat(CompanyStatus.from(List.of(item(approved), item(approved)))).isEqualTo(CompanyStatus.COMPLETED);
    }
}
