package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Python simulator.py betiginde calisan HTTP kontrol dinleyicisine
 * manuel/otonom mod ve vana kapatma komutlarini asenkron olarak gonderen istemci.
 */
@Component
@RequiredArgsConstructor
public class SimulatorKontrolClient {

    private static final Logger log = LoggerFactory.getLogger(SimulatorKontrolClient.class);
    private static final String KONTROL_YOLU = "/api/control";

    private final WebClient simulatorWebClient;

    public void komutGonder(String istasyonId, String komut) {
        simulatorWebClient.post()
                .uri(KONTROL_YOLU)
                .bodyValue(Map.of("istasyonId", istasyonId, "komut", komut))
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        yanit -> log.info("Simulator kontrol komutu iletildi: istasyon={}, komut={}", istasyonId, komut),
                        hata -> log.warn("Simulator kontrol komutu iletilemedi: istasyon={}, komut={}, hata={}",
                                istasyonId, komut, hata.getMessage())
                );
    }
}
