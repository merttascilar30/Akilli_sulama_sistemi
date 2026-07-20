package com.example.backend.service;

import com.example.backend.dto.KuralMotoruIstekDto;
import com.example.backend.dto.SensorMetrikRequestDto;
import com.example.backend.entity.Istasyon;
import com.example.backend.entity.SensorMetrik;
import com.example.backend.repository.IstasyonRepository;
import com.example.backend.repository.SensorMetrikRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Arka planda her 250 ms'de bir calisan zamanlanmis gorev.
 * Kuyruktaki sensor verilerini bosaltip sensorMetrikRepository.saveAll ile
 * tek seferde veritabanina yazar, ardindan her kaydi /topic/live-metrics
 * kanaligina anlik olarak yayinlar. Son olarak her istasyon icin en son
 * olcumu Python Kural Motoru servisine gonderip sulama karari alir.
 */
@Component
@RequiredArgsConstructor
public class SensorMetrikTopluYaziciGorevi {

    private static final Logger log = LoggerFactory.getLogger(SensorMetrikTopluYaziciGorevi.class);
    private static final String CANLI_METRIK_KANALI = "/topic/live-metrics";

    private final SensorMetrikKuyrugu sensorMetrikKuyrugu;
    private final SensorMetrikRepository sensorMetrikRepository;
    private final IstasyonRepository istasyonRepository;
    private final KuralMotoruClient kuralMotoruClient;
    private final OtonomVanaTetikleyici otonomVanaTetikleyici;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Value("${kural-motoru.et0-varsayilan}")
    private BigDecimal et0VarsayilanDegeri;

    @Scheduled(fixedRate = 250)
    public void kuyruguBosaltVeTopluYaz() {
        List<SensorMetrikRequestDto> yazilacakVeriler = sensorMetrikKuyrugu.tumunuBosalt();
        if (yazilacakVeriler.isEmpty()) {
            return;
        }

        List<SensorMetrik> kaydedilenMetrikler = sensorMetrikRepository.saveAll(
                yazilacakVeriler.stream().map(this::dtoDenEntityUret).toList());

        log.info("{} adet sensor verisi toplu olarak veritabanina yazildi.", kaydedilenMetrikler.size());

        kaydedilenMetrikler.forEach(metrik -> simpMessagingTemplate.convertAndSend(CANLI_METRIK_KANALI, metrik));

        kuralMotorunuTetikle(yazilacakVeriler);
    }

    private SensorMetrik dtoDenEntityUret(SensorMetrikRequestDto veri) {
        return SensorMetrik.builder()
                .istasyonId(veri.getIstasyonId())
                .nem(veri.getNem())
                .sicaklik(veri.getSicaklik())
                .kayitTarihi(OffsetDateTime.now())
                .build();
    }

    private void kuralMotorunuTetikle(List<SensorMetrikRequestDto> veriler) {
        Map<UUID, SensorMetrikRequestDto> istasyonBasinaSonOlcum = new LinkedHashMap<>();
        for (SensorMetrikRequestDto veri : veriler) {
            istasyonBasinaSonOlcum.put(veri.getIstasyonId(), veri);
        }

        istasyonBasinaSonOlcum.forEach((istasyonId, sonOlcum) ->
                istasyonRepository.findById(istasyonId).ifPresent(istasyon -> degerlendirVeUygula(istasyon, sonOlcum)));
    }

    private void degerlendirVeUygula(Istasyon istasyon, SensorMetrikRequestDto sonOlcum) {
        KuralMotoruIstekDto istek = KuralMotoruIstekDto.builder()
                .istasyonId(istasyon.getId().toString())
                .anlikNem(sonOlcum.getNem())
                .tarlaKapasitesi(istasyon.getTarlaKapasitesi())
                .solmaNoktasi(istasyon.getSolmaNoktasi())
                .et0(et0VarsayilanDegeri)
                .kc(istasyon.getBitkiKatsayisi())
                .build();

        kuralMotoruClient.degerlendir(istek)
                .subscribe(
                        yanit -> otonomVanaTetikleyici.kuralSonucunuUygula(istasyon.getId(), yanit),
                        hata -> log.warn("Kural motoru cagrisi basarisiz oldu, istasyon: {}, hata: {}",
                                istasyon.getId(), hata.getMessage())
                );
    }
}
