package com.example.backend.service;

import com.example.backend.dto.SensorMetrikRequestDto;
import com.example.backend.entity.SensorMetrik;
import com.example.backend.repository.SensorMetrikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Sensor metriklerinin senkron (naif) ve asenkron (kuyruga ekleyen) yazma
 * operasyonlarini yuruten servis implementasyonu.
 */
@Service
@RequiredArgsConstructor
public class SensorMetrikServiceImpl implements SensorMetrikService {

    private final SensorMetrikRepository sensorMetrikRepository;
    private final SensorMetrikKuyrugu sensorMetrikKuyrugu;

    @Override
    @Transactional
    public void senkronKaydet(SensorMetrikRequestDto sensorMetrikRequestDto) {
        SensorMetrik sensorMetrik = SensorMetrik.builder()
                .istasyonId(sensorMetrikRequestDto.getIstasyonId())
                .nem(sensorMetrikRequestDto.getNem())
                .sicaklik(sensorMetrikRequestDto.getSicaklik())
                .kayitTarihi(OffsetDateTime.now())
                .build();
        sensorMetrikRepository.save(sensorMetrik);
    }

    @Override
    public void kuyrugaEkle(SensorMetrikRequestDto sensorMetrikRequestDto) {
        sensorMetrikKuyrugu.ekle(sensorMetrikRequestDto);
    }
}
