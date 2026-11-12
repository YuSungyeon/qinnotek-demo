package com.qinnotek.submission.service;

import com.qinnotek.submission.config.AppProperties;
import com.qinnotek.submission.domain.Confidence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 추출된 문서 텍스트를 요청 자료에 배정한다. 외부 호출 없는 순수 로직.
 *
 * <p>사진 분류(브라우저 CLIP)와 같은 그리디 1:1 배정을 쓰되, 사진과 달리 확신이 낮으면 배정을 포기한다.
 * 사업자등록증이 임대차계약서 자리에 들어가는 것보다 "어느 항목인지 확인해 주세요"가 낫다.
 */
@Component
@RequiredArgsConstructor
public class DocumentClassifier {

    /** 서류 제목은 대개 첫 줄에 있다 - 앞부분 일치에 큰 가중치를 준다 */
    private static final int HEAD_CHARS = 300;
    private static final double W_HEAD = 3.0;
    private static final double W_BODY = 1.0;
    private static final double W_NAME = 1.5;
    private static final double W_FILENAME = 2.0;
    private static final double W_BIGRAM = 0.4;
    /** 본문 빈도의 수확 체감 기준 (3회 이상이면 만점) */
    private static final double FREQ_SATURATION = 3.0;
    private static final int BIGRAM_BODY_CHARS = 2000;

    private final AppProperties appProperties;

    /** 배정 대상 슬롯. */
    public record Slot(Long itemId, String name, String description, String keywords) {
    }

    /** 배정 대상 문서. */
    public record Doc(Long stagedId, String fileName, String text) {
    }

    /**
     * @param itemId     배정된 항목. 배정 포기 시 null
     * @param confidence HIGH/LOW/NONE
     */
    public record Assignment(Long stagedId, Long itemId, double score, double margin,
                             Confidence confidence, Long secondItemId) {
    }

    public List<Assignment> assign(List<Doc> docs, List<Slot> slots) {
        if (docs.isEmpty()) {
            return List.of();
        }
        if (slots.isEmpty()) {
            return docs.stream()
                    .map(d -> new Assignment(d.stagedId(), null, 0, 0, Confidence.NONE, null))
                    .toList();
        }

        Map<String, Double> idf = buildIdf(slots);
        double[][] matrix = new double[docs.size()][slots.size()];
        for (int d = 0; d < docs.size(); d++) {
            for (int s = 0; s < slots.size(); s++) {
                matrix[d][s] = score(docs.get(d), slots.get(s), idf);
            }
        }
        return greedyAssign(docs, slots, matrix);
    }

    // --- 점수 ---

    private double score(Doc doc, Slot slot, Map<String, Double> idf) {
        String text = normalize(doc.text());
        String head = text.length() > HEAD_CHARS ? text.substring(0, HEAD_CHARS) : text;
        String fileName = normalize(stripExtension(doc.fileName()));

        List<String> keywords = tokensOf(slot.keywords());
        String name = normalize(slot.name());

        double total = 0;

        for (String kw : keywords) {
            double weight = idf.getOrDefault(kw, 1.0);
            if (head.contains(kw)) {
                total += W_HEAD * weight;
            }
            int count = countOccurrences(text, kw);
            if (count > 0) {
                total += W_BODY * weight * Math.min(1.0, Math.log1p(count) / Math.log1p(FREQ_SATURATION));
            }
            if (!fileName.isEmpty() && fileName.contains(kw)) {
                total += W_FILENAME * weight;
            }
        }

        // 키워드를 등록하지 않은 항목도 명칭만으로 최소한의 매칭이 되도록 한다
        if (!name.isEmpty()) {
            if (head.contains(name)) {
                total += W_HEAD;
            }
            if (text.contains(name)) {
                total += W_NAME;
            }
            if (!fileName.isEmpty() && fileName.contains(name)) {
                total += W_FILENAME;
            }
        }

        // 형태소 분석기 없이 부분 일치를 흡수 (예: "임대차계약" ↔ "임대차계약서")
        String slotText = normalize(slot.name() + " " + nullToEmpty(slot.description()) + " " + nullToEmpty(slot.keywords()));
        String bodyForBigram = text.length() > BIGRAM_BODY_CHARS ? text.substring(0, BIGRAM_BODY_CHARS) : text;
        total += W_BIGRAM * bigramCosine(slotText, bodyForBigram.isEmpty() ? fileName : bodyForBigram);

        return total;
    }

    /** 여러 항목에 공통으로 등장하는 키워드("서류", "사본")의 영향력을 낮춘다. */
    private Map<String, Double> buildIdf(List<Slot> slots) {
        Map<String, Integer> df = new HashMap<>();
        for (Slot slot : slots) {
            for (String token : new HashSet<>(tokensOf(slot.keywords()))) {
                df.merge(token, 1, Integer::sum);
            }
        }
        int n = slots.size();
        Map<String, Double> idf = new HashMap<>();
        df.forEach((token, count) -> idf.put(token, Math.log(1.0 + (double) n / count)));
        return idf;
    }

    // --- 배정 ---

    private List<Assignment> greedyAssign(List<Doc> docs, List<Slot> slots, double[][] matrix) {
        record Pair(int docIdx, int slotIdx, double score) {
        }
        List<Pair> flat = new ArrayList<>();
        for (int d = 0; d < docs.size(); d++) {
            for (int s = 0; s < slots.size(); s++) {
                flat.add(new Pair(d, s, matrix[d][s]));
            }
        }
        flat.sort(Comparator.comparingDouble(Pair::score).reversed());

        var doc = appProperties.getDocument();
        Set<Integer> usedDoc = new HashSet<>();
        Set<Integer> usedSlot = new HashSet<>();
        Assignment[] result = new Assignment[docs.size()];

        for (Pair p : flat) {
            if (usedDoc.contains(p.docIdx()) || usedSlot.contains(p.slotIdx())) {
                continue;
            }
            usedDoc.add(p.docIdx());
            usedSlot.add(p.slotIdx());

            int secondIdx = bestOther(matrix[p.docIdx()], p.slotIdx());
            double second = secondIdx < 0 ? 0 : matrix[p.docIdx()][secondIdx];
            double margin = p.score() - second;

            Confidence confidence;
            Long itemId;
            if (p.score() < doc.getAbstainScore()) {
                // 어느 항목인지 알 수 없다 - 고객이 직접 고르게 한다
                confidence = Confidence.NONE;
                itemId = null;
            } else if (p.score() < doc.getLowScore() || margin < doc.getLowMargin()) {
                confidence = Confidence.LOW;
                itemId = slots.get(p.slotIdx()).itemId();
            } else {
                confidence = Confidence.HIGH;
                itemId = slots.get(p.slotIdx()).itemId();
            }

            result[p.docIdx()] = new Assignment(
                    docs.get(p.docIdx()).stagedId(), itemId, p.score(), margin, confidence,
                    secondIdx < 0 ? null : slots.get(secondIdx).itemId());
        }

        // 슬롯보다 문서가 많으면 남는 문서가 생긴다
        List<Assignment> assignments = new ArrayList<>(docs.size());
        for (int d = 0; d < docs.size(); d++) {
            assignments.add(result[d] != null ? result[d]
                    : new Assignment(docs.get(d).stagedId(), null, 0, 0, Confidence.NONE, null));
        }
        return assignments;
    }

    private static int bestOther(double[] row, int exclude) {
        int best = -1;
        for (int i = 0; i < row.length; i++) {
            if (i == exclude) {
                continue;
            }
            if (best < 0 || row[i] > row[best]) {
                best = i;
            }
        }
        return best;
    }

    // --- 텍스트 유틸 ---

    /** 공백/기호를 모두 제거해 "사업자 등록증"과 "사업자등록증"을 같게 만든다. */
    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-z가-힣]", "");
    }

    private static List<String> tokensOf(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String raw : commaSeparated.split("[,\\n]")) {
            String token = normalize(raw);
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static int countOccurrences(String text, String token) {
        if (text.isEmpty() || token.isEmpty()) {
            return 0;
        }
        int count = 0;
        int from = 0;
        while (true) {
            int at = text.indexOf(token, from);
            if (at < 0) {
                return count;
            }
            count++;
            from = at + token.length();
        }
    }

    private static double bigramCosine(String a, String b) {
        Map<String, Integer> va = bigrams(a);
        Map<String, Integer> vb = bigrams(b);
        if (va.isEmpty() || vb.isEmpty()) {
            return 0;
        }
        double dot = 0;
        for (var e : va.entrySet()) {
            Integer other = vb.get(e.getKey());
            if (other != null) {
                dot += (double) e.getValue() * other;
            }
        }
        return dot == 0 ? 0 : dot / (norm(va) * norm(vb));
    }

    private static Map<String, Integer> bigrams(String s) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i + 2 <= s.length(); i++) {
            map.merge(s.substring(i, i + 2), 1, Integer::sum);
        }
        return map;
    }

    private static double norm(Map<String, Integer> v) {
        double sum = 0;
        for (int c : v.values()) {
            sum += (double) c * c;
        }
        return Math.sqrt(sum);
    }

    private static String stripExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
