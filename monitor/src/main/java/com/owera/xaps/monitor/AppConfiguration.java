package com.owera.xaps.monitor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration(proxyBeanMethods = false)
public class AppConfiguration {
    /**
     * Custom executor for asynchronous monitoring tasks.
     * This prevents the main scheduler from being blocked by HTTP calls.
     */
    @Bean(name = "monitorTaskExecutor")
    public Executor monitorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("monitor-exec-");
        executor.initialize();
        return executor;
    }
}
