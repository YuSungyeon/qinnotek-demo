package com.qinnotek.submission.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 문서 변환 워커. soffice 동시 실행 수(기본 1)와 맞춰 스레드를 1개만 둔다.
 * 큐가 가득 차면 호출 스레드가 직접 처리해 작업이 유실되지 않게 한다.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean("documentExecutor")
    public Executor documentExecutor(AppProperties appProperties) {
        int threads = Math.max(1, appProperties.getDocument().getConvertConcurrency());
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("doc-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
