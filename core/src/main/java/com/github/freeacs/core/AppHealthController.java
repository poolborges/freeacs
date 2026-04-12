package com.github.freeacs.core;

import com.github.freeacs.common.http.HealthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.OffsetDateTime;


@RestController("coreAppHealthController")
public class AppHealthController {

    @Autowired
    private BuildProperties buildProperties;

    /**
     * Endpoint for monitoring. Returns the status in JSON format.
     */
    @GetMapping("/ok")
    public HealthResponse health() {
        return new HealthResponse(
                "UP",
                buildProperties.getVersion(),
                buildProperties.getName(),
                "ok",
                OffsetDateTime.now()
        );
    }
}