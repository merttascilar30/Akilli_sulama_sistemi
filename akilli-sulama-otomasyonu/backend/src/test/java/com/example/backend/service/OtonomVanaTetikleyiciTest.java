package com.example.backend.service;

import com.example.backend.dto.KuralMotoruYanitDto;
import com.example.backend.entity.Istasyon;
import com.example.backend.repository.IstasyonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtonomVanaTetikleyiciTest {

    @Mock
    private IstasyonRepository istasyonRepository;

    @Mock
    private IstasyonKontrolModuServisi istasyonKontrolModuServisi;

    @Mock
    private VanaLogYayinci vanaLogYayinci;

    @InjectMocks
    private OtonomVanaTetikleyici otonomVanaTetikleyici;

    @Test
    public void testTetikle_VanaKapaliyken_SulamaGerekli_VanaAcilmali() {
        // Given: İstasyon otonom modda, başlangıçta bilinen bir durumu yok (varsayılan KAPALI), kural motoru "sulama gerekli" diyor
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(true);
        mockResponse.setAnlikNem(new BigDecimal("25.0"));

        when(istasyonKontrolModuServisi.otonomModundaMi(testIstasyonId)).thenReturn(true);

        Istasyon testIstasyon = Istasyon.builder()
                .id(testIstasyonId)
                .tarlaKapasitesi(new BigDecimal("40.0"))
                .build();
        when(istasyonRepository.findById(testIstasyonId)).thenReturn(Optional.of(testIstasyon));

        // When: Tetikleyici servis çalıştırılıyor
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, mockResponse);

        // Then: Vana logu 1 (AÇIK) durumuyla kaydedilip yayınlanmalı
        verify(vanaLogYayinci, times(1)).kaydetVeYayinla(testIstasyonId, 1, "OTONOM");
    }

    @Test
    public void testTetikle_VanaKapaliyken_SulamaGerekliDegil_IslemGerekliDegil() {
        // Given: Vana zaten kapalı (varsayılan) ve kural motoru "sulama gerekmiyor" diyor
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(false);

        when(istasyonKontrolModuServisi.otonomModundaMi(testIstasyonId)).thenReturn(true);
        when(istasyonRepository.findById(testIstasyonId)).thenReturn(Optional.empty());

        // When: Tetikleyici servis çalıştırılıyor
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, mockResponse);

        // Then: Durum değişmediği için hiçbir kayıt/yayın yapılmamalı
        verify(vanaLogYayinci, never()).kaydetVeYayinla(any(), anyInt(), anyString());
    }

    @Test
    public void testTetikle_AyniKararTekrarGeldiginde_SpamLogAtilmamali() {
        // Given: İlk çağrıda vana açılıyor
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(true);
        mockResponse.setAnlikNem(new BigDecimal("25.0"));

        when(istasyonKontrolModuServisi.otonomModundaMi(testIstasyonId)).thenReturn(true);

        Istasyon testIstasyon = Istasyon.builder()
                .id(testIstasyonId)
                .tarlaKapasitesi(new BigDecimal("40.0"))
                .build();
        when(istasyonRepository.findById(testIstasyonId)).thenReturn(Optional.of(testIstasyon));

        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, mockResponse);

        // When: Aynı "sulama gerekli" kararı arka arkaya tekrar geliyor (250ms'lik döngüde olduğu gibi)
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, mockResponse);
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, mockResponse);

        // Then: Durum değişmediği için sadece ilk çağrıda kayıt/yayın yapılmalı, spam log oluşmamalı
        verify(vanaLogYayinci, times(1)).kaydetVeYayinla(any(), anyInt(), anyString());
    }

    @Test
    public void testTetikle_VanaAcikkenTarlaKapasitesineUlasildi_VanaOtonomKapanmali() {
        // Given: Vana zaten açık ve kural motoru hâlâ "sulama gerekli" diyor, ama toprak nemi tarla kapasitesine ulaştı
        UUID testIstasyonId = UUID.randomUUID();

        when(istasyonKontrolModuServisi.otonomModundaMi(testIstasyonId)).thenReturn(true);

        Istasyon testIstasyon = Istasyon.builder()
                .id(testIstasyonId)
                .tarlaKapasitesi(new BigDecimal("40.0"))
                .build();
        when(istasyonRepository.findById(testIstasyonId)).thenReturn(Optional.of(testIstasyon));

        KuralMotoruYanitDto acmaYaniti = new KuralMotoruYanitDto();
        acmaYaniti.setSulamaGerekli(true);
        acmaYaniti.setAnlikNem(new BigDecimal("25.0"));
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, acmaYaniti);

        KuralMotoruYanitDto doygunlukYaniti = new KuralMotoruYanitDto();
        doygunlukYaniti.setSulamaGerekli(true);
        doygunlukYaniti.setAnlikNem(new BigDecimal("40.0"));

        // When: Toprak nemi tarla kapasitesine ulaştığında tekrar değerlendiriliyor
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, doygunlukYaniti);

        // Then: Kural motorunun kararından bağımsız olarak vana otonom şekilde KAPALI (0) konuma geçmeli
        verify(vanaLogYayinci, times(1)).kaydetVeYayinla(testIstasyonId, 1, "OTONOM");
        verify(vanaLogYayinci, times(1)).kaydetVeYayinla(testIstasyonId, 0, "OTONOM");
    }

    @Test
    public void testTetikle_IstasyonManuelModdaysa_OtonomKararUygulanmamali() {
        // Given: İstasyon kullanıcı tarafından MANUEL moda alınmış
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(true);
        mockResponse.setAnlikNem(new BigDecimal("25.0"));

        when(istasyonKontrolModuServisi.otonomModundaMi(testIstasyonId)).thenReturn(false);

        // When: Tetikleyici servis çalıştırılıyor
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, mockResponse);

        // Then: Otonom karar tamamen yok sayılmalı, istasyon repository'sine bile gidilmemeli
        verify(vanaLogYayinci, never()).kaydetVeYayinla(any(), anyInt(), anyString());
        verify(istasyonRepository, never()).findById(any());
    }
}
