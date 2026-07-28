package com.example.backend.service;

import com.example.backend.dto.KuralMotoruYanitDto;
import com.example.backend.entity.Istasyon;
import com.example.backend.entity.VanaLog;
import com.example.backend.repository.IstasyonRepository;
import com.example.backend.repository.VanaLogRepository;
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
    private VanaLogRepository vanaLogRepository;

    @Mock
    private IstasyonKontrolModuServisi istasyonKontrolModuServisi;

    @Mock
    private VanaLogYayinci vanaLogYayinci;

    @InjectMocks
    private OtonomVanaTetikleyici otonomVanaTetikleyici;

    @Test
    public void testTetikle_VanaKapaliyken_SulamaGerekli_VanaAcilmali() {
        // Given: İstasyon otonom modda, veri tabanında henüz hiç log yok (varsayılan KAPALI), kural motoru "sulama gerekli" diyor
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(true);
        mockResponse.setAnlikNem(new BigDecimal("25.0"));

        when(istasyonKontrolModuServisi.otonomModundaMi(testIstasyonId)).thenReturn(true);
        when(vanaLogRepository.findTopByIstasyonIdOrderByIdDesc(testIstasyonId)).thenReturn(Optional.empty());

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
        // Given: Veri tabanında hiç log yok (varsayılan KAPALI) ve kural motoru "sulama gerekmiyor" diyor
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(false);

        when(istasyonKontrolModuServisi.otonomModundaMi(testIstasyonId)).thenReturn(true);
        when(vanaLogRepository.findTopByIstasyonIdOrderByIdDesc(testIstasyonId)).thenReturn(Optional.empty());
        when(istasyonRepository.findById(testIstasyonId)).thenReturn(Optional.empty());

        // When: Tetikleyici servis çalıştırılıyor
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, mockResponse);

        // Then: Durum değişmediği için hiçbir kayıt/yayın yapılmamalı
        verify(vanaLogYayinci, never()).kaydetVeYayinla(any(), anyInt(), anyString());
    }

    @Test
    public void testTetikle_AyniKararTekrarGeldiginde_SpamLogAtilmamali() {
        // Given: İlk çağrıda veri tabanında log yok, vana açılıyor; sonraki çağrılarda
        // veri tabanındaki gerçek son log artık AÇIK (1) olarak dönüyor (kalıcı yazım simülasyonu)
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(true);
        mockResponse.setAnlikNem(new BigDecimal("25.0"));

        when(istasyonKontrolModuServisi.otonomModundaMi(testIstasyonId)).thenReturn(true);

        VanaLog acikLog = VanaLog.builder().durum(1).tetiklemeTipi("OTONOM").build();
        when(vanaLogRepository.findTopByIstasyonIdOrderByIdDesc(testIstasyonId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(acikLog));

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
        // Given: Veri tabanındaki ilk durum boş, ilk çağrıda vana açılıyor; ikinci çağrıda
        // gerçek son log artık AÇIK (1) dönüyor ve kural motoru hâlâ "sulama gerekli" diyor,
        // ama toprak nemi tarla kapasitesine ulaştı
        UUID testIstasyonId = UUID.randomUUID();

        when(istasyonKontrolModuServisi.otonomModundaMi(testIstasyonId)).thenReturn(true);

        VanaLog acikLog = VanaLog.builder().durum(1).tetiklemeTipi("OTONOM").build();
        when(vanaLogRepository.findTopByIstasyonIdOrderByIdDesc(testIstasyonId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(acikLog));

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

        // Then: Otonom karar tamamen yok sayılmalı, ne istasyon ne de vana logu repository'sine gidilmemeli
        verify(vanaLogYayinci, never()).kaydetVeYayinla(any(), anyInt(), anyString());
        verify(istasyonRepository, never()).findById(any());
        verify(vanaLogRepository, never()).findTopByIstasyonIdOrderByIdDesc(any());
    }

    @Test
    public void testTetikle_ManuelKapatmaSonrasiOtonomModaDonulduğunde_GercekVeriTabaniDurumunaGoreVanaTekrarAcilmali() {
        // Given: Bu senaryo "State Cache Invalidation" hatasını doğrudan test eder.
        // Kullanıcı ManuelVanaKontrolServisi üzerinden vanayı MANUEL olarak kapatmış;
        // veri tabanındaki GERÇEK en son log artık MANUEL/KAPALI (0) durumundadır.
        // Sistem tekrar OTONOM moda alınıp kural motoru "sulama gerekli" dediğinde,
        // eski (yerel önbelleğe dayalı) mantık bunu fark edemeyip sessiz kalırdı.
        UUID testIstasyonId = UUID.randomUUID();

        when(istasyonKontrolModuServisi.otonomModundaMi(testIstasyonId)).thenReturn(true);

        VanaLog manuelKapaliLog = VanaLog.builder().durum(0).tetiklemeTipi("MANUEL").build();
        when(vanaLogRepository.findTopByIstasyonIdOrderByIdDesc(testIstasyonId))
                .thenReturn(Optional.of(manuelKapaliLog));

        Istasyon testIstasyon = Istasyon.builder()
                .id(testIstasyonId)
                .tarlaKapasitesi(new BigDecimal("40.0"))
                .build();
        when(istasyonRepository.findById(testIstasyonId)).thenReturn(Optional.of(testIstasyon));

        KuralMotoruYanitDto acmaYaniti = new KuralMotoruYanitDto();
        acmaYaniti.setSulamaGerekli(true);
        acmaYaniti.setAnlikNem(new BigDecimal("25.0"));

        // When: Otonom mod tekrar devreye girip kural motoru "sulama gerekli" dediğinde
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, acmaYaniti);

        // Then: Gerçek son durumun KAPALI olduğu veri tabanından okunduğu için
        // vana tekrar AÇIK (1) olarak loglanıp yayınlanmalı; arayüz yeşile dönmeli
        verify(vanaLogYayinci, times(1)).kaydetVeYayinla(testIstasyonId, 1, "OTONOM");
    }
}
