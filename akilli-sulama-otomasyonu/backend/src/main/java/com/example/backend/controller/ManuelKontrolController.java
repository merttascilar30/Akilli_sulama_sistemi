package com.example.backend.controller;

import com.example.backend.dto.ManuelKontrolIstekDto;
import com.example.backend.service.ManuelVanaKontrolServisi;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * Arayuzden STOMP uzerinden /app/manual-control adresine gonderilen
 * manuel vana kontrol isteklerini karsilayan WebSocket uc noktasi.
 */
@Controller
@RequiredArgsConstructor
public class ManuelKontrolController {

    private final ManuelVanaKontrolServisi manuelVanaKontrolServisi;

    @MessageMapping("/manual-control")
    public void manuelKontroluIsle(ManuelKontrolIstekDto istek) {
        manuelVanaKontrolServisi.komutuIsle(istek);
    }
}
