package com.github.freeacs.utils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Força o uso do HttpURLConnection nativo do Java,
        // ignorando os conflitos do Apache HttpClient no classpath
        return new RestTemplate(new SimpleClientHttpRequestFactory());
    }
}