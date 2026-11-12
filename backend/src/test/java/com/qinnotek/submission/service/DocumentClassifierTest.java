package com.qinnotek.submission.service;

import com.qinnotek.submission.config.AppProperties;
import com.qinnotek.submission.domain.Confidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentClassifierTest {

    private static final DocumentClassifier.Slot LICENSE = new DocumentClassifier.Slot(
            1L, "사업자등록증", "사업자등록증 사본", "사업자등록증,등록번호,대표자,개업연월일");
    private static final DocumentClassifier.Slot LEASE = new DocumentClassifier.Slot(
            2L, "임대차계약서", "임대차계약서 사본", "임대차계약서,임대인,임차인,보증금,월세");
    private static final DocumentClassifier.Slot BANKBOOK = new DocumentClassifier.Slot(
            3L, "통장사본", "입금 계좌 통장 사본", "통장사본,예금주,계좌번호,은행");

    private DocumentClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new DocumentClassifier(new AppProperties());
    }

    @Test
    void 본문_내용으로_각_문서를_올바른_항목에_배정한다() {
        var docs = List.of(
                new DocumentClassifier.Doc(10L, "scan001.pdf",
                        "사업자등록증\n등록번호: 123-45-67890\n대표자: 홍길동\n개업연월일: 2020년 3월 1일"),
                new DocumentClassifier.Doc(11L, "scan002.pdf",
                        "임대차계약서\n임대인: 김영희\n임차인: 홍길동\n보증금 5,000만원 월세 200만원"),
                new DocumentClassifier.Doc(12L, "scan003.pdf",
                        "통장사본\n예금주: 홍길동\n계좌번호: 110-123-456789\n은행: 신한은행"));

        var byDoc = assignmentsById(docs, List.of(LICENSE, LEASE, BANKBOOK));

        assertThat(byDoc.get(10L).itemId()).isEqualTo(1L);
        assertThat(byDoc.get(11L).itemId()).isEqualTo(2L);
        assertThat(byDoc.get(12L).itemId()).isEqualTo(3L);
        assertThat(byDoc.values()).allMatch(a -> a.confidence() == Confidence.HIGH);
    }

    @Test
    void 텍스트가_없는_스캔문서는_파일명으로_배정한다() {
        var docs = List.of(
                new DocumentClassifier.Doc(20L, "임대차 계약서_2024.pdf", ""),
                new DocumentClassifier.Doc(21L, "사업자등록증사본.pdf", ""));

        var byDoc = assignmentsById(docs, List.of(LICENSE, LEASE, BANKBOOK));

        // 파일명의 공백은 정규화로 흡수된다 ("임대차 계약서" → "임대차계약서")
        assertThat(byDoc.get(20L).itemId()).isEqualTo(2L);
        assertThat(byDoc.get(21L).itemId()).isEqualTo(1L);
    }

    @Test
    void 단서가_전혀_없으면_배정을_포기한다() {
        var docs = List.of(new DocumentClassifier.Doc(30L, "IMG_0421.pdf", "        "));

        var assignments = classifier.assign(docs, List.of(LICENSE, LEASE, BANKBOOK));

        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).confidence()).isEqualTo(Confidence.NONE);
        assertThat(assignments.get(0).itemId()).isNull();
    }

    @Test
    void 슬롯보다_문서가_많으면_남는_문서는_배정하지_않는다() {
        var docs = List.of(
                new DocumentClassifier.Doc(40L, "사업자등록증.pdf", "사업자등록증 등록번호 대표자"),
                new DocumentClassifier.Doc(41L, "임대차계약서.pdf", "임대차계약서 임대인 임차인"));

        var byDoc = assignmentsById(docs, List.of(LICENSE));

        assertThat(byDoc.get(40L).itemId()).isEqualTo(1L);
        assertThat(byDoc.get(41L).itemId()).isNull();
    }

    @Test
    void 배정할_슬롯이_없으면_전부_포기한다() {
        var docs = List.of(new DocumentClassifier.Doc(50L, "사업자등록증.pdf", "사업자등록증"));

        var assignments = classifier.assign(docs, List.of());

        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).confidence()).isEqualTo(Confidence.NONE);
    }

    @Test
    void 한_문서를_두_항목에_동시에_배정하지_않는다() {
        // 두 문서 모두 사업자등록증처럼 보여도 1:1 로만 배정된다
        var docs = List.of(
                new DocumentClassifier.Doc(60L, "a.pdf", "사업자등록증 등록번호 대표자"),
                new DocumentClassifier.Doc(61L, "b.pdf", "사업자등록증 등록번호 대표자"));

        var assignments = classifier.assign(docs, List.of(LICENSE, LEASE));

        assertThat(assignments.stream().map(DocumentClassifier.Assignment::itemId).filter(java.util.Objects::nonNull))
                .doesNotHaveDuplicates();
    }

    @Test
    void 공백과_기호를_무시하고_같은_말로_본다() {
        assertThat(DocumentClassifier.normalize("사업자 등록증"))
                .isEqualTo(DocumentClassifier.normalize("사업자등록증"))
                .isEqualTo("사업자등록증");
        assertThat(DocumentClassifier.normalize("Lease-Contract_2024!")).isEqualTo("leasecontract2024");
    }

    private Map<Long, DocumentClassifier.Assignment> assignmentsById(
            List<DocumentClassifier.Doc> docs, List<DocumentClassifier.Slot> slots) {
        return classifier.assign(docs, slots).stream()
                .collect(java.util.stream.Collectors.toMap(
                        DocumentClassifier.Assignment::stagedId, Function.identity()));
    }
}
