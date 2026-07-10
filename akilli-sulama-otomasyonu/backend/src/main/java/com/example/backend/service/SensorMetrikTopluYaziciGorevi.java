package com.example.backend.service;

import com.example.backend.dto.SensorMetrikRequestDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Arka planda her 250 ms'de bir calisan zamanlanmis gorev.
 * Kuyruktaki sensor verilerini bosaltip JdbcTemplate ile tek seferde
 * (batch insert) PostgreSQL'e gomer. Boylece yuksek frekansli veri akisinda
 * her istek icin ayri bir veritabani baglantisi acilmaz.
 */
@Component
@RequiredArgsConstructor
public class SensorMetrikTopluYaziciGorevi {

    private static final Logger log = LoggerFactory.getLogger(SensorMetrikTopluYaziciGorevi.class);

    private static final String TOPLU_EKLEME_SORGUSU =
            "INSERT INTO sensor_metrikleri (istasyon_id, nem, sicaklik, kayit_tarihi) VALUES (?, ?, ?, ?)";

    private final SensorMetrikKuyrugu sensorMetrikKuyrugu;
    private final JdbcTemplate jdbcTemplate;

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
    }
}
