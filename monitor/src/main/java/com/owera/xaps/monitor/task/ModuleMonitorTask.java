package com.owera.xaps.monitor.task;

import com.owera.xaps.monitor.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ModuleMonitorTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModuleMonitorTask.class);

  // Using a Map because Records are immutable; we must replace the entry to update it.
  private static final Map<String, MonitorInfo> monitorMap = new ConcurrentHashMap<>();

  static {
    String[] modules = {"core", "stun", "syslog", "tr069", "web", "webservice"};
    for (String module : modules) {
      monitorMap.put(module, new MonitorInfo(module));
    }
  }

  private final Properties properties;
  private final MonitorService monitorService;

  public ModuleMonitorTask(Properties properties, MonitorService monitorService) {
    this.properties = properties;
    this.monitorService = monitorService;
  }

  @Scheduled(cron = "0 * * * * *")
  public void execute() {
    String urlBase = Properties.URL_BASE;

    List<CompletableFuture<Void>> futures = monitorMap.values().stream()
            .map(info -> {
              String moduleUrl = properties.get("monitor.url." + info.module());
              if (moduleUrl == null) {
                moduleUrl = urlBase + info.module() + "/ok";
              }

              return monitorService.checkModuleStatus(moduleUrl)
                      .thenAccept(result -> {
                        // Create a new record instance with updated data
                        MonitorInfo updatedInfo = new MonitorInfo(
                                info.module(),
                                result.status(),
                                result.url(),
                                result.version(),
                                result.errorMessage(),
                                OffsetDateTime.now()
                        );
                        monitorMap.put(info.module(), updatedInfo);
                      });
            })
            .toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
  }

  public static Collection<MonitorInfo> getMonitorInfoSet() {
    return new TreeSet<>(monitorMap.values());
  }
}
