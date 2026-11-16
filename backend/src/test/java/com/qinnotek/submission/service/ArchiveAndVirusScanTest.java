package com.qinnotek.submission.service;

import com.qinnotek.submission.config.AppProperties;
import com.qinnotek.submission.domain.Company;
import com.qinnotek.submission.domain.Requirement;
import com.qinnotek.submission.domain.SubmissionItem;
import com.qinnotek.submission.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 업로드 → 바이러스 검사(clamd) → NAS 기업 폴더 복사.
 */
class ArchiveAndVirusScanTest {

    @Test
    void 바이러스면_400_검사서버가_없으면_503_깨끗하면_통과() throws Exception {
        assertThatCode(() -> scannerFor(fakeClamd("stream: OK")).check(content(), "a.pdf"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> scannerFor(fakeClamd("stream: Eicar-Test-Signature FOUND")).check(content(), "a.pdf"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        int closedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            closedPort = s.getLocalPort();
        }
        assertThatThrownBy(() -> scannerFor(closedPort).check(content(), "a.pdf"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void 기업_폴더에_요청자료명으로_복사하고_재업로드는_덮어쓴다(@TempDir Path uploads, @TempDir Path nas) throws Exception {
        AppProperties props = new AppProperties();
        props.getUpload().setDir(uploads.toString());
        props.getArchive().setDir(nas.toString());
        ArchiveService archive = new ArchiveService(props, new FileStorageService(props, new VirusScanner(props)));

        archive.createCompanyFolder("㈜가나다");
        archive.awaitIdle();
        assertThat(nas.resolve("㈜가나다")).isDirectory();

        // 요청자료명의 '/' 는 경로가 되지 않게 '_' 로 바뀐다
        SubmissionItem item = new SubmissionItem(new Company("㈜가나다"), new Requirement("통장/사본", "설명", null, null));
        Files.writeString(uploads.resolve("abc.jpg"), "jpg");
        item.attachPhoto("abc.jpg", "통장.jpg", uploads.resolve("abc.jpg").toString());
        archive.copy(item);
        archive.awaitIdle();
        assertThat(nas.resolve("㈜가나다/통장_사본.jpg")).hasContent("jpg");

        // 확장자가 다른 파일로 다시 올리면 이전 사본은 지워진다
        Files.writeString(uploads.resolve("def.png"), "png");
        item.attachPhoto("def.png", "통장.png", uploads.resolve("def.png").toString());
        archive.copy(item);
        archive.awaitIdle();
        assertThat(nas.resolve("㈜가나다/통장_사본.png")).hasContent("png");
        assertThat(nas.resolve("㈜가나다/통장_사본.jpg")).doesNotExist();

        // 기업명을 바꾸면 폴더째 옮긴다. ".." 같은 이름도 루트 밖으로 나가지 않는다
        archive.renameCompanyFolder("㈜가나다", "../가나다상사");
        archive.awaitIdle();
        assertThat(nas.resolve("_가나다상사/통장_사본.png")).hasContent("png");
    }

    private static ByteArrayInputStream content() {
        return new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));
    }

    private static VirusScanner scannerFor(int port) {
        AppProperties props = new AppProperties();
        props.getVirusScan().setEnabled(true);
        props.getVirusScan().setPort(port);
        return new VirusScanner(props);
    }

    /** INSTREAM 을 끝까지 읽고 정해진 응답을 한 번 돌려주는 가짜 clamd. 포트를 반환한다. */
    private static int fakeClamd(String reply) throws Exception {
        ServerSocket server = new ServerSocket(0);
        Thread t = new Thread(() -> {
            try (server; Socket s = server.accept()) {
                DataInputStream in = new DataInputStream(s.getInputStream());
                byte[] cmd = new byte["zINSTREAM\0".length()];
                in.readFully(cmd);
                assertThat(new String(cmd, StandardCharsets.US_ASCII)).isEqualTo("zINSTREAM\0");
                for (int n; (n = in.readInt()) > 0; ) {
                    in.readFully(new byte[n]);
                }
                s.getOutputStream().write((reply + "\0").getBytes(StandardCharsets.US_ASCII));
            } catch (Exception ignored) {
                // 테스트 쪽 단언이 실패를 드러낸다
            }
        });
        t.setDaemon(true);
        t.start();
        return server.getLocalPort();
    }
}
