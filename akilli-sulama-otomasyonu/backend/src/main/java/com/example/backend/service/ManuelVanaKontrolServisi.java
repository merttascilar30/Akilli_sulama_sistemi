package com.example.backend.service;

import com.example.backend.dto.ManuelKontrolIstekDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Arayuzden /app/manual-control kanali uzerinden gelen manuel kontrol
 * isteklerini isleyen servis. Istasyonun kontrol modunu gunceller, Python
 * simulator.py betigine kontrol komutunu iletir ve gerekiyorsa vana_loglari
 * tablosuna manuel kapatma kaydi girer.
 */
@Service
@RequiredArgsConstructor
public class ManuelVanaKontrolServisi {

    private static final String KOMUT_KAPAT = "KAPAT";
    private static final int VANA_KAPALI = 0;
    private static final String TETIKLEME_TIPI_MANUEL = "MANUEL";

    private final IstasyonKontrolModuServisi istasyonKontrolModuServisi;
    private final SimulatorKontrolClient simulatorKontrolClient;
    private final VanaLogYayinci vanaLogYayinci;

    public void komutuIsle(ManuelKontrolIstekDto istek) {
        UUID istasyonId = UUID.fromString(istek.getIstasyonId());
        String komut = istek.getKomut() != null ? istek.getKomut().toUpperCase() : "";

        if (IstasyonKontrolModuServisi.MOD_OTONOM.equals(komut)) {
            istasyonKontrolModuServisi.modAyarla(istasyonId, IstasyonKontrolModuServisi.MOD_OTONOM);
            simulatorKontrolClient.komutGonder(istek.getIstasyonId(), IstasyonKontrolModuServisi.MOD_OTONOM);
            return;
        }

        // MANUEL veya KAPAT komutlari, otonom kural motorunu bu istasyon icin devre disi birakir
        istasyonKontrolModuServisi.modAyarla(istasyonId, IstasyonKontrolModuServisi.MOD_MANUEL);
        simulatorKontrolClient.komutGonder(istek.getIstasyonId(), komut);

        if (KOMUT_KAPAT.equals(komut) || IstasyonKontrolModuServisi.MOD_MANUEL.equals(komut)) {
            vanaLogYayinci.kaydetVeYayinla(istasyonId, VANA_KAPALI, TETIKLEME_TIPI_MANUEL);
        }
    }
}
