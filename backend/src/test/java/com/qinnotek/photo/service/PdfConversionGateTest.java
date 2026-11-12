package com.qinnotek.photo.service;

import com.qinnotek.photo.config.AppProperties;
import com.qinnotek.photo.domain.TextStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 포맷별 PDF 변환 + 한글 텍스트 추출 게이트.
 *
 * <p>LibreOffice 가 없는 환경에서는 전부 skip 된다(로컬/CI 어디서든 안전).
 * 실행: ./gradlew test --tests '*PdfConversionGateTest*'
 */
class PdfConversionGateTest {

    /** Flat ODF - 별도 도구 없이 만들 수 있어 다른 포맷의 원본으로 쓴다. */
    private static final String FODT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                             xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
                             office:version="1.2" office:mimetype="application/vnd.oasis.opendocument.text">
              <office:body><office:text>
                <text:p>사업자등록증</text:p>
                <text:p>등록번호: 123-45-67890  대표자: 홍길동</text:p>
                <text:p>개업연월일: 2020년 3월 1일</text:p>
              </office:text></office:body>
            </office:document>
            """;

    private static PdfConversionService conversion;
    private static DocumentTextExtractor extractor;

    @BeforeAll
    static void setUp(@TempDir Path uploadDir) {
        AppProperties props = new AppProperties();
        props.getUpload().setDir(uploadDir.toString());
        conversion = new PdfConversionService(props);
        conversion.init();
        extractor = new DocumentTextExtractor();
    }

    @Test
    void 워드_docx_를_PDF로_변환하고_한글을_추출한다() throws IOException {
        assumeSofficeAvailable();
        assertKoreanSurvives(makeSample("docx"), "docx");
    }

    @Test
    void 구형_워드_doc_를_PDF로_변환하고_한글을_추출한다() throws IOException {
        assumeSofficeAvailable();
        assertKoreanSurvives(makeSample("doc"), "doc");
    }

    @Test
    void 원본이_PDF면_변환하지_않고_그대로_사용한다() throws IOException {
        assumeSofficeAvailable();
        Path pdf = makeSample("pdf");
        var result = conversion.convert(pdf, "pdf");

        assertThat(result.success()).isTrue();
        assertThat(result.converted()).isFalse();
        assertThat(result.pdfPath()).isEqualTo(pdf);
    }

    @Test
    void 변환기가_없으면_예외대신_실패결과를_돌려준다() {
        AppProperties props = new AppProperties();
        props.getDocument().setSofficeBin("/nonexistent/soffice");
        PdfConversionService broken = new PdfConversionService(props);
        broken.init();

        assertThat(broken.isAvailable()).isFalse();
        var result = broken.convert(Path.of("/tmp/whatever.docx"), "docx");
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isNotBlank();
    }

    // --- helpers ---

    private static void assumeSofficeAvailable() {
        assumeTrue(conversion.isAvailable(),
                "LibreOffice(soffice) 미설치 - 변환 게이트를 건너뜁니다");
    }

    /** 한글이 두부(□)로 깨지지 않고 살아있는지까지 확인한다. */
    private void assertKoreanSurvives(Path source, String ext) {
        var result = conversion.convert(source, ext);
        assertThat(result.success())
                .as("%s → PDF 변환", ext)
                .isTrue();

        var extracted = extractor.extract(result.pdfPath());
        assertThat(extracted.status()).isEqualTo(TextStatus.OK);
        assertThat(extracted.text())
                .as("%s 변환 결과에서 한글 추출 (폰트 누락 시 여기서 실패)", ext)
                .contains("사업자등록증")
                .contains("홍길동");
    }

    /** fodt 를 원하는 포맷으로 변환해 테스트용 원본을 만든다. */
    private Path makeSample(String targetExt) throws IOException {
        Path dir = Files.createTempDirectory("gate-src-");
        Path fodt = dir.resolve("sample.fodt");
        Files.writeString(fodt, FODT, StandardCharsets.UTF_8);

        var made = conversion.convert(fodt, "fodt");
        if ("pdf".equals(targetExt)) {
            return made.pdfPath();
        }
        // fodt → 대상 포맷은 soffice 를 직접 한 번 더 호출한다(테스트 전용 경로)
        return convertTo(fodt, targetExt);
    }

    private Path convertTo(Path source, String targetExt) throws IOException {
        Path outDir = Files.createTempDirectory("gate-out-");
        Path profile = outDir.resolve("profile");
        String bin = System.getenv().getOrDefault("SOFFICE_BIN",
                "/Applications/LibreOffice.app/Contents/MacOS/soffice");
        if (!Files.isExecutable(Path.of(bin))) {
            bin = "/usr/bin/soffice";
        }
        ProcessBuilder pb = new ProcessBuilder(bin,
                "--headless", "--invisible", "--nologo", "--nodefault", "--norestore", "--nolockcheck",
                "-env:UserInstallation=file://" + profile.toAbsolutePath(),
                "--convert-to", targetExt, "--outdir", outDir.toString(), source.toString());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.environment().put("HOME", outDir.toString());
        try {
            pb.start().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return outDir.resolve("sample." + targetExt);
    }
}
