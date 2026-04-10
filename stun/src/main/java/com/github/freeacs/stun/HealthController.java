package com.github.freeacs.stun;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/ok")
    public String health() {
        return "FREEACSOK";
    }
}
