package com.reviewbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Value("${async.review.core-pool-size:5}")
    private int corePoolSize;

    @Value("${async.review.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${async.review.queue-capacity:100}")
    private int queueCapacity;

    @Value("${async.review.thread-name-prefix:review-worker-}")
    private String threadNamePrefix;

    @Bean(name = "reviewTaskExecutor")
    public Executor reviewTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
