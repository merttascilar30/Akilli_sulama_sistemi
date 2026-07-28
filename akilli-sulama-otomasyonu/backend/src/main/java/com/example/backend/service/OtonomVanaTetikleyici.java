package com.example.backend.service;

import com.example.backend.dto.KuralMotoruYanitDto;
import com.example.backend.entity.Istasyon;
import com.example.backend.entity.VanaLog;
import com.example.backend.repository.IstasyonRepository;
import com.example.backend.repository.VanaLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kural motorundan gelen degerlendirme sonucuna gore vana_loglari tablosuna
 * otonom tetikleme kaydi giren servis.
 *
 * Istasyonun "su anki" vana durumu, yerel bir bellek ici onbellege degil,
 * VanaLogRepository uzerinden o istasyon icin atilmis EN SON logun gercek
 * durumuna bakilarak belirlenir. Boylece ManuelVanaKontrolServisi gibi baska
 * bir servis araya MANUEL bir kapatma logu soktuğunda (State Cache
 * Invalidation), bu servis eski/yanlis bir onbellek degerine gore karar
 * vermez; her zaman veritabanindaki gercek son duruma gore hareket eder.
 * Veritabanina yazma ve WebSocket yayini SADECE bu gercek durumla hedef
 * durum farkli oldugunda tetiklenir; boylece ayni kararin her cagride
 * tekrar tekrar loglanmasi (spam) engellenir. Ayrica vana zaten ACIK
 * durumdayken toprak nemi tarla kapasitesine ulastiysa, kural motorunun
 * kararindan bagimsiz olarak vana otonom sekilde KAPALI konuma gecirilir.
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
    private final VanaLogRepository vanaLogRepository;
    private final IstasyonKontrolModuServisi istasyonKontrolModuServisi;
    private final VanaLogYayinci vanaLogYayinci;

    public void kuralSonucunuUygula(UUID istasyonId, KuralMotoruYanitDto kuralMotoruYaniti) {
        if (!istasyonKontrolModuServisi.otonomModundaMi(istasyonId)) {
            return;
        }

        int mevcutDurum = sonBilinenVanaDurumu(istasyonId);
        int hedefDurum = kuralMotoruYaniti.isSulamaGerekli() ? VANA_ACIK : VANA_KAPALI;

        Istasyon istasyon = istasyonRepository.findById(istasyonId).orElse(null);

        if (mevcutDurum == VANA_ACIK && tarlaKapasitesineUlasildi(istasyon, kuralMotoruYaniti.getAnlikNem())) {
            hedefDurum = VANA_KAPALI;
        }

        if (hedefDurum == mevcutDurum) {
            return;
        }

        vanaLogYayinci.kaydetVeYayinla(istasyonId, hedefDurum, TETIKLEME_TIPI_OTONOM);
    }

    /**
     * Istasyonun gercek son vana durumunu vana_loglari tablosundaki EN SON
     * kayittan okur. Hic log yoksa vana varsayilan olarak KAPALI kabul edilir.
     */
    private int sonBilinenVanaDurumu(UUID istasyonId) {
        return vanaLogRepository.findTopByIstasyonIdOrderByIdDesc(istasyonId)
                .map(VanaLog::getDurum)
                .orElse(VANA_KAPALI);
    }

    private boolean tarlaKapasitesineUlasildi(Istasyon istasyon, BigDecimal anlikNem) {
        if (istasyon == null || istasyon.getTarlaKapasitesi() == null || anlikNem == null) {
            return false;
        }
        return anlikNem.compareTo(istasyon.getTarlaKapasitesi()) >= 0;
    }
}
