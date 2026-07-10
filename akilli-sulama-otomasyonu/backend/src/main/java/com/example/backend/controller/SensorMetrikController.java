package com.example.backend.controller;

import com.example.backend.dto.SensorMetrikRequestDto;
import com.example.backend.service.SensorMetrikService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulatorden gelen yuksek frekansli sensor verilerini kabul eden REST API uc noktalari.
 * Iki farkli yazma stratejisini kiyaslamak icin kasitli olarak ayri iki uc barindirir.
 */
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class SensorMetrikController {

    private final SensorMetrikService sensorMetrikService;

    /**
     * Naif/baseline durum: gelen veriyi dogrudan veritabanina yazar.
     * Bu islem tamamlanana kadar ana thread bloklanir, yuksek frekansta darbogaz olusturur.
     */
    @PostMapping("/sync")
    public ResponseEntity<Void> senkronYaz(@Valid @RequestBody SensorMetrikRequestDto sensorMetrikRequestDto) {
        sensorMetrikService.senkronKaydet(sensorMetrikRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Yuksek performansli durum: veriyi dogrudan veritabanina yazmadan
     * thread-safe kuyruga atar ve hemen doner. Gercek yazma islemi arka plandaki
     * zamanlanmis toplu yazici tarafindan her 250 ms'de bir gerceklestirilir.
     */
    @PostMapping("/async")
    public ResponseEntity<Void> asenkronYaz(@Valid @RequestBody SensorMetrikRequestDto sensorMetrikRequestDto) {
        sensorMetrikService.kuyrugaEkle(sensorMetrikRequestDto);
        return ResponseEntity.accepted().build();
    }
}
