package com.example.backend.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Her istasyonun anlik kontrol modunu (OTONOM veya MANUEL) thread-safe
 * bir bellek ici harita uzerinde tutan servis. Bir istasyon MANUEL moda
 * alindiginda, otonom kural motoru tetikleyicisi o istasyon icin devre disi
 * birakilir; karar verme yetkisi tamamen kullaniciya gecer.
 */
@Component
public class IstasyonKontrolModuServisi {

    public static final String MOD_OTONOM = "OTONOM";
    public static final String MOD_MANUEL = "MANUEL";

    private final Map<String, String> istasyonModlari = new ConcurrentHashMap<>();

    public void modAyarla(UUID istasyonId, String mod) {
        istasyonModlari.put(istasyonId.toString(), mod);
    }

    /**
     * Istasyon icin herhangi bir mod kaydedilmemisse varsayilan olarak OTONOM kabul edilir.
     */
    public boolean otonomModundaMi(UUID istasyonId) {
        return !MOD_MANUEL.equals(istasyonModlari.get(istasyonId.toString()));
    }
}
