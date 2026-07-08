package com.example.backend.controller;

import com.example.backend.dto.IstasyonRequestDto;
import com.example.backend.dto.IstasyonResponseDto;
import com.example.backend.service.IstasyonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Istasyon kaynaklari icin REST API uc noktalari.
 */
@RestController
@RequestMapping("/api/istasyonlar")
@RequiredArgsConstructor
public class IstasyonController {

    private final IstasyonService istasyonService;

    @PostMapping
    public ResponseEntity<IstasyonResponseDto> istasyonEkle(@Valid @RequestBody IstasyonRequestDto istasyonRequestDto) {
        IstasyonResponseDto olusturulanIstasyon = istasyonService.istasyonEkle(istasyonRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(olusturulanIstasyon);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IstasyonResponseDto> istasyonGuncelle(@PathVariable UUID id,
                                                                 @Valid @RequestBody IstasyonRequestDto istasyonRequestDto) {
        IstasyonResponseDto guncellenenIstasyon = istasyonService.istasyonGuncelle(id, istasyonRequestDto);
        return ResponseEntity.ok(guncellenenIstasyon);
    }

    @GetMapping
    public ResponseEntity<List<IstasyonResponseDto>> tumIstasyonlariListele() {
        return ResponseEntity.ok(istasyonService.tumIstasyonlariListele());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IstasyonResponseDto> istasyonGetir(@PathVariable UUID id) {
        return ResponseEntity.ok(istasyonService.istasyonGetir(id));
    }
}
