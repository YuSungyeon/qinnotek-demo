package com.qinnotek.submission.health;

import com.qinnotek.submission.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Status;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadStorageHealthIndicatorTest {

    @Test
    void 업로드_폴더에_쓸_수_있으면_UP(@TempDir Path dir) {
        assertThat(indicatorFor(dir).health().getStatus()).isEqualTo(Status.UP);
        assertThat(dir.toFile().list()).as("확인용 파일은 남기지 않는다").isEmpty();
    }

    @Test
    void 업로드_폴더에_쓸_수_없으면_DOWN(@TempDir Path dir) {
        Path missing = dir.resolve("not-mounted");
        assertThat(indicatorFor(missing).health().getStatus()).isEqualTo(Status.DOWN);
    }

    private UploadStorageHealthIndicator indicatorFor(Path tmpRoot) {
        FileStorageService storage = mock(FileStorageService.class);
        when(storage.getTmpRoot()).thenReturn(tmpRoot);
        return new UploadStorageHealthIndicator(storage);
    }
}
