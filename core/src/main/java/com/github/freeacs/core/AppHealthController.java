package com.github.freeacs.core;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppHealthController {

    @GetMapping("/ok")
    public String health() {
        return "ok";
    }
}
