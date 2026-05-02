package com.smartlearn.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Virtual thread executor configuration for AI and async tasks.
 */
@Configuration
public class VirtualThreadConfig {

    @Bean("aiExecutor")
    public ExecutorService aiVirtualThreadExecutor() {
        return Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("ai-worker-", 0).factory()
        );
    }

    @Bean("taskExecutor")
    public ExecutorService generalVirtualThreadExecutor() {
        return Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("task-worker-", 0).factory()
        );
    }
}
