package com.rapidphoto.uploader.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution.
 * Used for image processing and other background tasks.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Task executor for async image processing.
     * Configured with appropriate pool sizes for concurrent image processing.
     */
    @Bean(name = "imageProcessingExecutor")
    public Executor imageProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ImageProcessing-");
        executor.initialize();
        return executor;
    }
}

