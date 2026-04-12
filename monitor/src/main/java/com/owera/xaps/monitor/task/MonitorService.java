package com.owera.xaps.monitor.task;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.concurrent.CompletableFuture;

@Service
public class MonitorService {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("")
            .build();

    @Async("monitorTaskExecutor")
    public CompletableFuture<MonitorResult> checkModuleStatus(String url) {
        try {
            MonitorResult result = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(MonitorResult.class);

            if (result != null && "UP".equalsIgnoreCase(result.status())) {
                return CompletableFuture.completedFuture(result);
            }
            return CompletableFuture.completedFuture(
                    new MonitorResult("ERROR", "Unexpected response content", "", url)
            );
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    new MonitorResult("ERROR", "Connection failed: " + e.getMessage(), "", url)
            );
        }
    }
}
