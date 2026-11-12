package com.qinnotek.submission.service;

import com.qinnotek.submission.domain.TextStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * PDF 에서 분류용 텍스트를 뽑는다. 모든 포맷은 LibreOffice 로 PDF 변환을 거친 뒤 이 경로를 탄다.
 *
 * <p>힙이 320MB 인 운영 환경을 고려해 앞 3페이지 / 20,000자까지만 읽고,
 * PDFBox 캐시도 힙 대신 임시 파일을 쓰도록 한다.
 */
@Slf4j
@Service
public class DocumentTextExtractor {

    private static final int MAX_PAGES = 3;
    /**
     * MySQL 의 TEXT 는 65,535 <b>바이트</b>다. 한글은 utf8mb4 에서 3바이트,
     * 이모지 등은 4바이트라 글자 수만 보면 넘칠 수 있다. 최악의 경우(4바이트)에도
     * 들어가도록 12,000자로 자른다 - 분류에는 앞부분만으로 충분하다.
     */
    private static final int MAX_CHARS = 12_000;

    public record Extracted(String text, TextStatus status) {
    }

    public Extracted extract(Path pdf) {
        try (var read = new RandomAccessReadBufferedFile(pdf.toFile());
             PDDocument doc = Loader.loadPDF(read, "", null, null,
                     IOUtils.createTempFileOnlyStreamCache())) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(MAX_PAGES, Math.max(1, doc.getNumberOfPages())));

            String text = stripper.getText(doc);
            if (text == null || text.isBlank()) {
                // 스캔 이미지만 있는 PDF - 파일명으로만 분류할 수 있다
                return new Extracted("", TextStatus.NO_TEXT);
            }
            return new Extracted(text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text,
                    TextStatus.OK);
        } catch (Exception e) {
            log.warn("PDF 텍스트 추출 실패: {}", pdf.getFileName(), e);
            return new Extracted("", TextStatus.ERROR);
        }
    }
}
