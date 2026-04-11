package com.github.freeacs.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class Properties {

  @Value("${reports:Basic}")
  private String reports;

  @Value("${staging:false}")
  private boolean staging;

  @Value("${shellscript.limit:7}")
  private Integer shellScriptLimit;

  @Value("${completed.job.limit:40}")
  private Integer completedJobLimit;

  @Value("${shellscript.poolsize:4}")
  private Integer shellScriptPoolSize;

  @Value("${syslog.cleanup:normal}")
  private String syslogCleanup;

  @Value("${server.servlet.context-path:/core}")
  private String contextPath;

  private final Map<Integer, Integer> syslogSeverityLimit = new HashMap<>();


  private final Environment env;

    public Properties(Environment env) {
        this.env = env;
    }

    @PostConstruct
  private void init() {
    // Carrega syslog.severity.0.limit até syslog.severity.7.limit conforme definido no arquivo
    for (int i = 0; i <= 7; i++) {
      String key = "syslog.severity." + i + ".limit";
      if (env.containsProperty(key)) {
        syslogSeverityLimit.put(i, env.getProperty(key, Integer.class));
      }
    }
  }

  public Integer getSyslogSeverityLimit(int severity) {
    return syslogSeverityLimit.get(severity);
  }

  public String getReports() { return reports; }
  public boolean isStaging() { return staging; }
  public Integer getShellScriptLimit() { return shellScriptLimit; }
  public Integer getCompletedJobLimit() { return completedJobLimit; }
  public Integer getShellScriptPoolSize() { return shellScriptPoolSize; }
  public String getSyslogCleanup() { return syslogCleanup; }
  public String getContextPath() { return contextPath; }

  // Exemplo de como acessar o host do syslog se necessário
  @Value("${syslog.server.host:localhost}")
  private String syslogServerHost;
  public String getSyslogServerHost() { return syslogServerHost; }
}
