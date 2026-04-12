package com.github.freeacs;

import com.github.freeacs.common.util.Sleep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Slf4j
@Configuration
@ComponentScan(
        basePackages = "com.github.freeacs",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.github\\.freeacs\\.core\\..*"
        )
)
@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.warn("Shutdown Hook is running!");
            Sleep.terminateApplication();
        }));
    }
}
