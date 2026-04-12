package com.owera.xaps.monitor.task;

/**
 * Immutable data carrier for monitoring results.
 */
public record MonitorResult(
        String status,
        String errorMessage,
        String version,
        String url
) {}
