package com.example.backend.service;

import com.example.backend.dto.KuralMotoruYanitDto;
import com.example.backend.entity.Istasyon;
import com.example.backend.repository.IstasyonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
 *
 * Istasyon kullanici tarafindan MANUEL moda alinmissa, bu servis o istasyon
 * icin devre disi kalir; karar verme yetkisi tamamen kullaniciya gecer.
 */
@Service
@RequiredArgsConstructor
public class OtonomVanaTetikleyici {

    private static final int VANA_ACIK = 1;
    private static final int VANA_KAPALI = 0;
    private static final String TETIKLEME_TIPI_OTONOM = "OTONOM";

    private final IstasyonRepository istasyonRepository;
    private final IstasyonKontrolModuServisi istasyonKontrolModuServisi;
    private final VanaLogYayinci vanaLogYayinci;

    private final Map<String, Integer> sonVanaDurumlari = new ConcurrentHashMap<>();

    public void kuralSonucunuUygula(UUID istasyonId, KuralMotoruYanitDto kuralMotoruYaniti) {
        if (!istasyonKontrolModuServisi.otonomModundaMi(istasyonId)) {
            return;
        }

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

        vanaLogYayinci.kaydetVeYayinla(istasyonId, hedefDurum, TETIKLEME_TIPI_OTONOM);
        sonVanaDurumlari.put(istasyonAnahtari, hedefDurum);
    }

    private boolean tarlaKapasitesineUlasildi(Istasyon istasyon, BigDecimal anlikNem) {
        if (istasyon == null || istasyon.getTarlaKapasitesi() == null || anlikNem == null) {
            return false;
        }
        return anlikNem.compareTo(istasyon.getTarlaKapasitesi()) >= 0;
    }
}
