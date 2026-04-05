package com.github.freeacs.syslogserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Properties {

  @Value("${min-syslog-db-commit-delay:5000}")
  private Integer minSyslogdbCommitDelay;

  @Value("${max-syslog-db-commit-queue:1000}")
  private Integer maxSyslogdbCommitQueue;

  @Value("${max-message-pr-minute:10000}")
  private Integer maxMessagesPerMinute;

  @Value("${min-free-disk-space:100}")
  private Integer minFreeDiskSpace;

  @Value("${unknown-units:allow}")
  private String unknownUnitsAction;

  @Value("${max-failover-message-age:24}")
  private Integer maxFailoverMessageAge;

  @Value("${max-syslogdb-threads:1}")
  private Integer maxSyslogdbThreads;

  @Value("${max-message-in-duplicate-buffer:100000}")
  private Integer maxMessagesInDuplicateBuffer;

  @Value("${max-messages-in-buffer:100000}")
  private Integer maxMessagesInBuffer;

  @Value("${receive-buffer-size:10240}")
  private Integer receiveBufferSize;

  @Value("${failover-process-interval:30}")
  private Integer failoverProcessInterval;

  @Value("${port:9116}")
  private Integer port;

  @Value("${simulation:false}")
  private boolean simulation;

  // Construtor padrão para o Spring
  public Properties() {}


  public Integer getMinSyslogdbCommitDelay() { return minSyslogdbCommitDelay; }
  public Integer getMaxSyslogdbCommitQueue() { return maxSyslogdbCommitQueue; }
  public Integer getMaxMessagesPerMinute() { return maxMessagesPerMinute; }
  public Integer getMinFreeDiskSpace() { return minFreeDiskSpace; }
  public String getUnknownUnitsAction() { return unknownUnitsAction; }
  public Integer getMaxFailoverMessageAge() { return maxFailoverMessageAge; }
  public Integer getMaxSyslogdbThreads() { return maxSyslogdbThreads; }
  public Integer getMaxMessagesInDuplicateBuffer() { return maxMessagesInDuplicateBuffer; }
  public Integer getMaxMessagesInBuffer() { return maxMessagesInBuffer; }
  public Integer getReceiveBufferSize() { return receiveBufferSize; }
  public Integer getFailoverProcessInterval() { return failoverProcessInterval; }
  public Integer getPort() { return port; }
  public boolean isSimulation() { return simulation; }

  public static String getDeviceIdPattern(int index) {
    return null;
  }
}
