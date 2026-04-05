package com.github.freeacs.syslogserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final SyslogServlet syslogServlet;

    public HealthController(SyslogServlet syslogServlet) {
        this.syslogServlet = syslogServlet;
    }

    @GetMapping("/ok")
    public String health() {
        return syslogServlet.health();
    }
}
