package com.owera.xaps.monitor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class Properties {

  public static Long RETRY_SECS;
  public static String URL_BASE;

  @Value("${monitor.urlbase:http://localhost/}")
  private String monitorUrlBase;

  @Value("${monitor.retrysec:300}")
  private Long monitorRetrySec;

  @Value("${server.servlet.context-path:/monitor}")
  private String contextPath;

  @Value("${server.port:8080}")
  private int serverPort;


  @PostConstruct
  private void initStaticFields() {
    URL_BASE = monitorUrlBase.endsWith("/") ? monitorUrlBase : monitorUrlBase + "/";
    RETRY_SECS = monitorRetrySec;
  }

  public String getMonitorURLBase() {
    return URL_BASE;
  }

  public long getRetrySeconds() {
    return RETRY_SECS;
  }

  public String getContextPath() {
    return contextPath;
  }

  public int getServerPort() {
    return serverPort;
  }

  public String get(String key) {
    return null;
  }
}
