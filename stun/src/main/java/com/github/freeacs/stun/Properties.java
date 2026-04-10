package com.github.freeacs.stun;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.Getter;

@Getter
@Setter
@Component
public class Properties {

  @Value("${kick.expect-port-forwarding:false}")
  private boolean expectPortForwarding;

  @Value("${test.runwithstun:true}")
  private boolean runWithStun;

  @Value("${secondary.ip:0.0.0.0}")
  private String secondaryIp;

  @Value("${primary.ip:0.0.0.0}")
  private String primaryIp;

  @Value("${secondary.port:3479}")
  private Integer secondaryPort;

  @Value("${primary.port:3478}")
  private Integer primaryPort;

  @Value("${kick.interval:1000}")
  private Integer kickInterval;

  @Value("${kick.check-public-ip:false}")
  private boolean checkPublicIp;

  @Value("${kick.rescan:60}")
  private Integer kickRescan;

  @Value("${server.servlet.context-path:/stun}")
  private String contextPath;

  public Properties() {}
}
