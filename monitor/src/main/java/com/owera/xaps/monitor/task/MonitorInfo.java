package com.owera.xaps.monitor.task;

import java.time.OffsetDateTime;

/**
 * Immutable record representing the state of a monitored module.
 */
public record MonitorInfo(
        String module,
        String status,
        String url,
        String version,
        String errorMessage,
        OffsetDateTime lastUpdate
) implements Comparable<MonitorInfo> {

  public MonitorInfo(String module) {
    this(module, "PENDING", null, null, null, null);
  }

  @Override
  public int compareTo(MonitorInfo o) {
    return this.module.compareTo(o.module);
  }
}
