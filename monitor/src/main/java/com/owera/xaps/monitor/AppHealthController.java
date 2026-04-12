package com.owera.xaps.monitor;


import com.owera.xaps.monitor.http.HealthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.OffsetDateTime;

@RestController
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