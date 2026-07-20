package com.example.backend.service;

import com.example.backend.dto.KuralMotoruYanitDto;
import com.example.backend.entity.VanaLog;
import com.example.backend.repository.VanaLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Kural motorundan gelen degerlendirme sonucuna gore vana_loglari tablosuna
 * otonom tetikleme kaydi giren ve kaydi /topic/vana-loglari kanaligina
 * anlik olarak yayinlayan servis.
 */
@Service
@RequiredArgsConstructor
public class OtonomVanaTetikleyici {

    private static final int VANA_ACIK = 1;
    private static final String TETIKLEME_TIPI_OTONOM = "OTONOM";
    private static final String VANA_LOG_KANALI = "/topic/vana-loglari";

    private final VanaLogRepository vanaLogRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Transactional
    public void kuralSonucunuUygula(UUID istasyonId, KuralMotoruYanitDto kuralMotoruYaniti) {
        if (!kuralMotoruYaniti.isSulamaGerekli()) {
            return;
        }

        VanaLog vanaLog = VanaLog.builder()
                .istasyonId(istasyonId)
                .durum(VANA_ACIK)
                .tetiklemeTipi(TETIKLEME_TIPI_OTONOM)
                .tarih(OffsetDateTime.now())
                .build();

        VanaLog kaydedilenVanaLog = vanaLogRepository.save(vanaLog);
        simpMessagingTemplate.convertAndSend(VANA_LOG_KANALI, kaydedilenVanaLog);
    }
}
