package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Python Kural Motoru ve Simulator servisleriyle iletisim kurmak icin kullanilan WebClient konfigurasyonu.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient kuralMotoruWebClient(@Value("${kural-motoru.base-url}") String kuralMotoruBaseUrl) {
        return WebClient.builder()
                .baseUrl(kuralMotoruBaseUrl)
                .build();
    }

    @Bean
    public WebClient simulatorWebClient(@Value("${simulator.control-base-url}") String simulatorControlBaseUrl) {
        return WebClient.builder()
                .baseUrl(simulatorControlBaseUrl)
                .build();
    }
}
