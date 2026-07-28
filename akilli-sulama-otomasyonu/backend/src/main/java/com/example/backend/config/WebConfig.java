package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Frontend panelinin (statik dosya veya farkli bir origin uzerinden acilan
 * index.html) REST API uc noktalarina (orn. /api/istasyonlar) tarayici
 * uzerinden erisebilmesi icin CORS yapilandirmasi.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String API_YOLU = "/api/**";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(API_YOLU)
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
