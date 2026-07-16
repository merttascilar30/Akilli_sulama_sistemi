package com.example.backend.service;

import com.example.backend.dto.KuralMotoruIstekDto;
import com.example.backend.dto.KuralMotoruYanitDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Python Kural Motoru servisine asenkron HTTP istekleri gonderen istemci.
 */
@Component
@RequiredArgsConstructor
public class KuralMotoruClient {

    private static final String DEGERLENDIRME_YOLU = "/api/rules/evaluate";

    private final WebClient kuralMotoruWebClient;

    public Mono<KuralMotoruYanitDto> degerlendir(KuralMotoruIstekDto kuralMotoruIstekDto) {
        return kuralMotoruWebClient.post()
                .uri(DEGERLENDIRME_YOLU)
                .bodyValue(kuralMotoruIstekDto)
                .retrieve()
                .bodyToMono(KuralMotoruYanitDto.class);
    }
}
