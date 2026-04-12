package com.github.freeacs.common.http;


import java.time.OffsetDateTime;

public record HealthResponse(
        String status,
        String version,
        String url,
        String errorMessage,
        OffsetDateTime dateTime
) {}
