package com.example.backend.service;

import com.example.backend.dto.IstasyonRequestDto;
import com.example.backend.dto.IstasyonResponseDto;
import com.example.backend.entity.Istasyon;
import org.springframework.stereotype.Component;

/**
 * Istasyon entity <-> DTO donusumlerini merkezilestiren mapper sinifi.
 */
@Component
public class IstasyonMapper {

    public Istasyon toEntity(IstasyonRequestDto dto) {
        return Istasyon.builder()
                .ad(dto.getAd())
                .koordinat(dto.getKoordinat())
                .tarlaKapasitesi(dto.getTarlaKapasitesi())
                .solmaNoktasi(dto.getSolmaNoktasi())
                .bitkiKatsayisi(dto.getBitkiKatsayisi())
                .build();
    }

    public void updateEntityFromDto(IstasyonRequestDto dto, Istasyon entity) {
        entity.setAd(dto.getAd());
        entity.setKoordinat(dto.getKoordinat());
        entity.setTarlaKapasitesi(dto.getTarlaKapasitesi());
        entity.setSolmaNoktasi(dto.getSolmaNoktasi());
        entity.setBitkiKatsayisi(dto.getBitkiKatsayisi());
    }

    public IstasyonResponseDto toResponseDto(Istasyon entity) {
        return IstasyonResponseDto.builder()
                .id(entity.getId())
                .ad(entity.getAd())
                .koordinat(entity.getKoordinat())
                .tarlaKapasitesi(entity.getTarlaKapasitesi())
                .solmaNoktasi(entity.getSolmaNoktasi())
                .bitkiKatsayisi(entity.getBitkiKatsayisi())
                .build();
    }
}
