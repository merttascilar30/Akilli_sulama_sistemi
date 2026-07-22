package com.example.backend.service;

import com.example.backend.dto.KuralMotoruYanitDto;
import com.example.backend.dto.VanaLogYayinDto;
import com.example.backend.entity.Istasyon;
import com.example.backend.entity.VanaLog;
import com.example.backend.repository.IstasyonRepository;
import com.example.backend.repository.VanaLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtonomVanaTetikleyiciTest {

    @Mock
    private VanaLogRepository vanaLogRepository;

    @Mock
    private IstasyonRepository istasyonRepository;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private OtonomVanaTetikleyici otonomVanaTetikleyici;

    @Test
    public void testTetikle_VanaKapaliyken_SulamaGerekli_VanaAcilmali() {
        // Given: İstasyon başlangıçta bilinen bir durumda değil (varsayılan KAPALI), kural motoru "sulama gerekli" diyor
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(true);
        mockResponse.setAnlikNem(new BigDecimal("25.0"));
        when(vanaLogRepository.save(any(VanaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Istasyon testIstasyon = Istasyon.builder()
                .id(testIstasyonId)
                .enlem(38.4192)
                .boylam(27.1287)
                .tarlaKapasitesi(new BigDecimal("40.0"))
                .build();
        when(istasyonRepository.findById(testIstasyonId)).thenReturn(Optional.of(testIstasyon));

        // When: Tetikleyici servis çalıştırılıyor
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, mockResponse);

        // Then: Veri tabanı repository'sinin "save" metodu tam 1 kez çağrılmalı
        ArgumentCaptor<VanaLog> vanaLogCaptor = ArgumentCaptor.forClass(VanaLog.class);
        verify(vanaLogRepository, times(1)).save(vanaLogCaptor.capture());

        VanaLog savedLog = vanaLogCaptor.getValue();
        assertEquals(testIstasyonId, savedLog.getIstasyonId());
        assertEquals(1, savedLog.getDurum());
        assertEquals("OTONOM", savedLog.getTetiklemeTipi());
        assertNotNull(savedLog.getTarih());

        // Then: Enlem/boylam bilgileriyle zenginleştirilmiş yayın /topic/vana-loglari kanalına gönderilmeli
        ArgumentCaptor<VanaLogYayinDto> yayinCaptor = ArgumentCaptor.forClass(VanaLogYayinDto.class);
        verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/vana-loglari"), yayinCaptor.capture());

        VanaLogYayinDto yayinlananVeri = yayinCaptor.getValue();
        assertEquals(testIstasyonId, yayinlananVeri.getIstasyonId());
        assertEquals(1, yayinlananVeri.getDurum());
        assertEquals("OTONOM", yayinlananVeri.getTetiklemeTipi());
        assertEquals(38.4192, yayinlananVeri.getEnlem());
        assertEquals(27.1287, yayinlananVeri.getBoylam());
    }

    @Test
    public void testTetikle_VanaKapaliyken_SulamaGerekliDegil_IslemGerekliDegil() {
        // Given: Vana zaten kapalı (varsayılan) ve kural motoru "sulama gerekmiyor" diyor
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(false);
        when(istasyonRepository.findById(testIstasyonId)).thenReturn(Optional.empty());

        // When: Tetikleyici servis çalıştırılıyor
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, mockResponse);

        // Then: Durum değişmediği için hiçbir kayıt atılmamalı ve yayın yapılmamalı
        verify(vanaLogRepository, never()).save(any(VanaLog.class));
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    public void testTetikle_AyniKararTekrarGeldiginde_SpamLogAtilmamali() {
        // Given: İlk çağrıda vana açılıyor
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(true);
        mockResponse.setAnlikNem(new BigDecimal("25.0"));
        when(vanaLogRepository.save(any(VanaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        verify(vanaLogRepository, times(1)).save(any(VanaLog.class));
        verify(simpMessagingTemplate, times(1)).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    public void testTetikle_VanaAcikkenTarlaKapasitesineUlasildi_VanaOtonomKapanmali() {
        // Given: Vana zaten açık ve kural motoru hâlâ "sulama gerekli" diyor, ama toprak nemi tarla kapasitesine ulaştı
        UUID testIstasyonId = UUID.randomUUID();

        KuralMotoruYanitDto acmaYaniti = new KuralMotoruYanitDto();
        acmaYaniti.setSulamaGerekli(true);
        acmaYaniti.setAnlikNem(new BigDecimal("25.0"));
        when(vanaLogRepository.save(any(VanaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Istasyon testIstasyon = Istasyon.builder()
                .id(testIstasyonId)
                .tarlaKapasitesi(new BigDecimal("40.0"))
                .build();
        when(istasyonRepository.findById(testIstasyonId)).thenReturn(Optional.of(testIstasyon));

        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, acmaYaniti);

        KuralMotoruYanitDto doygunlukYaniti = new KuralMotoruYanitDto();
        doygunlukYaniti.setSulamaGerekli(true);
        doygunlukYaniti.setAnlikNem(new BigDecimal("40.0"));

        // When: Toprak nemi tarla kapasitesine ulaştığında tekrar değerlendiriliyor
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, doygunlukYaniti);

        // Then: Kural motorunun kararından bağımsız olarak vana otonom şekilde KAPALI konuma geçmeli
        ArgumentCaptor<VanaLog> vanaLogCaptor = ArgumentCaptor.forClass(VanaLog.class);
        verify(vanaLogRepository, times(2)).save(vanaLogCaptor.capture());

        VanaLog sonKayit = vanaLogCaptor.getAllValues().get(1);
        assertEquals(0, sonKayit.getDurum());
        assertEquals("OTONOM", sonKayit.getTetiklemeTipi());
    }
}
