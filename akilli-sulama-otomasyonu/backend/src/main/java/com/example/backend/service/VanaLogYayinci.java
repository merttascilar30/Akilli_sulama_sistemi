package com.example.backend.service;

import com.example.backend.dto.VanaLogYayinDto;
import com.example.backend.entity.Istasyon;
import com.example.backend.entity.VanaLog;
import com.example.backend.repository.IstasyonRepository;
import com.example.backend.repository.VanaLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Vana durum degisikliklerini (otonom veya manuel kaynakli) vana_loglari
 * tablosuna kaydedip, istasyonun enlem/boylam bilgileriyle zenginlestirerek
 * /topic/vana-loglari kanaligina yayinlayan ortak servis.
 */
@Component
@RequiredArgsConstructor
public class VanaLogYayinci {

    private static final String VANA_LOG_KANALI = "/topic/vana-loglari";

    private final VanaLogRepository vanaLogRepository;
    private final IstasyonRepository istasyonRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Transactional
    public void kaydetVeYayinla(UUID istasyonId, int durum, String tetiklemeTipi) {
        Istasyon istasyon = istasyonRepository.findById(istasyonId).orElse(null);

        VanaLog kaydedilenVanaLog = vanaLogRepository.save(
                VanaLog.builder()
                        .istasyonId(istasyonId)
                        .durum(durum)
                        .tetiklemeTipi(tetiklemeTipi)
                        .tarih(OffsetDateTime.now())
                        .build());

        simpMessagingTemplate.convertAndSend(VANA_LOG_KANALI, vanaLogYayinDtoUret(kaydedilenVanaLog, istasyon));
    }

    private VanaLogYayinDto vanaLogYayinDtoUret(VanaLog vanaLog, Istasyon istasyon) {
        return VanaLogYayinDto.builder()
                .istasyonId(vanaLog.getIstasyonId())
                .durum(vanaLog.getDurum())
                .tetiklemeTipi(vanaLog.getTetiklemeTipi())
                .tarih(vanaLog.getTarih())
                .enlem(istasyon != null ? istasyon.getEnlem() : null)
                .boylam(istasyon != null ? istasyon.getBoylam() : null)
                .build();
    }
}
