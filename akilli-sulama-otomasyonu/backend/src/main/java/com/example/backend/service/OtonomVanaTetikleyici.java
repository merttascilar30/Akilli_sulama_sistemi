package com.example.backend.service;

import com.example.backend.dto.KuralMotoruYanitDto;
import com.example.backend.dto.VanaLogYayinDto;
import com.example.backend.entity.Istasyon;
import com.example.backend.entity.VanaLog;
import com.example.backend.repository.IstasyonRepository;
import com.example.backend.repository.VanaLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kural motorundan gelen degerlendirme sonucuna gore vana_loglari tablosuna
 * otonom tetikleme kaydi giren servis.
 *
 * Her istasyonun son bilinen vana durumu thread-safe bir bellek ici harita
 * (sonVanaDurumlari) uzerinde tutulur. Veritabanina yazma ve WebSocket
 * yayini SADECE durum degisikliginde tetiklenir; boylece ayni kararin
 * her cagride tekrar tekrar loglanmasi (spam) engellenir. Ayrica vana
 * zaten ACIK durumdayken toprak nemi tarla kapasitesine ulastiysa, kural
 * motorunun kararindan bagimsiz olarak vana otonom sekilde KAPALI konuma
 * gecirilir.
 */
@Service
@RequiredArgsConstructor
public class OtonomVanaTetikleyici {

    private static final int VANA_ACIK = 1;
    private static final int VANA_KAPALI = 0;
    private static final String TETIKLEME_TIPI_OTONOM = "OTONOM";
    private static final String VANA_LOG_KANALI = "/topic/vana-loglari";

    private final VanaLogRepository vanaLogRepository;
    private final IstasyonRepository istasyonRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    private final Map<String, Integer> sonVanaDurumlari = new ConcurrentHashMap<>();

    @Transactional
    public void kuralSonucunuUygula(UUID istasyonId, KuralMotoruYanitDto kuralMotoruYaniti) {
        String istasyonAnahtari = istasyonId.toString();
        int mevcutDurum = sonVanaDurumlari.getOrDefault(istasyonAnahtari, VANA_KAPALI);
        int hedefDurum = kuralMotoruYaniti.isSulamaGerekli() ? VANA_ACIK : VANA_KAPALI;

        Istasyon istasyon = istasyonRepository.findById(istasyonId).orElse(null);

        if (mevcutDurum == VANA_ACIK && tarlaKapasitesineUlasildi(istasyon, kuralMotoruYaniti.getAnlikNem())) {
            hedefDurum = VANA_KAPALI;
        }

        if (hedefDurum == mevcutDurum) {
            return;
        }

        VanaLog kaydedilenVanaLog = vanaLogRepository.save(
                VanaLog.builder()
                        .istasyonId(istasyonId)
                        .durum(hedefDurum)
                        .tetiklemeTipi(TETIKLEME_TIPI_OTONOM)
                        .tarih(OffsetDateTime.now())
                        .build());

        sonVanaDurumlari.put(istasyonAnahtari, hedefDurum);

        simpMessagingTemplate.convertAndSend(VANA_LOG_KANALI, vanaLogYayinDtoUret(kaydedilenVanaLog, istasyon));
    }

    private boolean tarlaKapasitesineUlasildi(Istasyon istasyon, BigDecimal anlikNem) {
        if (istasyon == null || istasyon.getTarlaKapasitesi() == null || anlikNem == null) {
            return false;
        }
        return anlikNem.compareTo(istasyon.getTarlaKapasitesi()) >= 0;
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
