package com.example.backend.service;

import com.example.backend.dto.KuralMotoruIstekDto;
import com.example.backend.dto.SensorMetrikRequestDto;
import com.example.backend.entity.Istasyon;
import com.example.backend.repository.IstasyonRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Arka planda her 250 ms'de bir calisan zamanlanmis gorev.
 * Kuyruktaki sensor verilerini bosaltip JdbcTemplate ile tek seferde
 * (batch insert) PostgreSQL'e gomer. Ardindan her istasyon icin en son
 * olcumu Python Kural Motoru servisine gonderip sulama karari alir.
 */
@Component
@RequiredArgsConstructor
public class SensorMetrikTopluYaziciGorevi {

    private static final Logger log = LoggerFactory.getLogger(SensorMetrikTopluYaziciGorevi.class);

    private static final String TOPLU_EKLEME_SORGUSU =
            "INSERT INTO sensor_metrikleri (istasyon_id, nem, sicaklik, kayit_tarihi) VALUES (?, ?, ?, ?)";

    private final SensorMetrikKuyrugu sensorMetrikKuyrugu;
    private final JdbcTemplate jdbcTemplate;
    private final IstasyonRepository istasyonRepository;
    private final KuralMotoruClient kuralMotoruClient;
    private final OtonomVanaTetikleyici otonomVanaTetikleyici;

    @Value("${kural-motoru.et0-varsayilan}")
    private BigDecimal et0VarsayilanDegeri;

    @Scheduled(fixedRate = 250)
    public void kuyruguBosaltVeTopluYaz() {
        List<SensorMetrikRequestDto> yazilacakVeriler = sensorMetrikKuyrugu.tumunuBosalt();
        if (yazilacakVeriler.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(TOPLU_EKLEME_SORGUSU, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SensorMetrikRequestDto veri = yazilacakVeriler.get(i);
                ps.setObject(1, veri.getIstasyonId());
                ps.setBigDecimal(2, veri.getNem());
                ps.setBigDecimal(3, veri.getSicaklik());
                ps.setTimestamp(4, Timestamp.from(OffsetDateTime.now().toInstant()));
            }

            @Override
            public int getBatchSize() {
                return yazilacakVeriler.size();
            }
        });

        log.info("{} adet sensor verisi toplu olarak veritabanina yazildi.", yazilacakVeriler.size());

        kuralMotorunuTetikle(yazilacakVeriler);
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
