package com.example.backend.service;

import com.example.backend.dto.IstasyonRequestDto;
import com.example.backend.dto.IstasyonResponseDto;
import com.example.backend.entity.Istasyon;
import com.example.backend.repository.IstasyonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Istasyon ekleme, guncelleme ve listeleme operasyonlarini yuruten servis implementasyonu.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IstasyonServiceImpl implements IstasyonService {

    private final IstasyonRepository istasyonRepository;
    private final IstasyonMapper istasyonMapper;

    @Override
    public IstasyonResponseDto istasyonEkle(IstasyonRequestDto istasyonRequestDto) {
        Istasyon yeniIstasyon = istasyonMapper.toEntity(istasyonRequestDto);
        Istasyon kaydedilenIstasyon = istasyonRepository.save(yeniIstasyon);
        return istasyonMapper.toResponseDto(kaydedilenIstasyon);
    }

    @Override
    public IstasyonResponseDto istasyonGuncelle(UUID id, IstasyonRequestDto istasyonRequestDto) {
        Istasyon mevcutIstasyon = bulYoksaHataFirlat(id);
        istasyonMapper.updateEntityFromDto(istasyonRequestDto, mevcutIstasyon);
        Istasyon guncellenenIstasyon = istasyonRepository.save(mevcutIstasyon);
        return istasyonMapper.toResponseDto(guncellenenIstasyon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IstasyonResponseDto> tumIstasyonlariListele() {
        return istasyonRepository.findAll()
                .stream()
                .map(istasyonMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public IstasyonResponseDto istasyonGetir(UUID id) {
        return istasyonMapper.toResponseDto(bulYoksaHataFirlat(id));
    }

    private Istasyon bulYoksaHataFirlat(UUID id) {
        return istasyonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Istasyon bulunamadi: " + id));
    }
}
