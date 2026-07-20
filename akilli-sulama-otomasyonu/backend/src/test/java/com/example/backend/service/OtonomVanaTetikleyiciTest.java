package com.example.backend.service;

import com.example.backend.dto.KuralMotoruYanitDto;
import com.example.backend.entity.VanaLog;
import com.example.backend.repository.VanaLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtonomVanaTetikleyiciTest {

    @Mock
    private VanaLogRepository vanaLogRepository;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private OtonomVanaTetikleyici otonomVanaTetikleyici;

    @Test
    public void testTetikle_VanaAcilmali_VanaLogKaydedilmeli() {
        // Given: Kural motorundan "sulama gerekli" kararı geliyor
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(true);
        when(vanaLogRepository.save(any(VanaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

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

        // Then: Kaydedilen log /topic/vana-loglari kanaligina yayinlanmali
        verify(simpMessagingTemplate, times(1)).convertAndSend("/topic/vana-loglari", savedLog);
    }

    @Test
    public void testTetikle_VanaKapatilmali_IslemGerekliDegil() {
        // Given: Kural motorundan "sulama gerekmiyor" kararı geliyor
        UUID testIstasyonId = UUID.randomUUID();
        KuralMotoruYanitDto mockResponse = new KuralMotoruYanitDto();
        mockResponse.setSulamaGerekli(false);

        // When: Tetikleyici servis çalıştırılıyor
        otonomVanaTetikleyici.kuralSonucunuUygula(testIstasyonId, mockResponse);

        // Then: Veri tabanına hiçbir kayıt atılmamalı ve yayin yapilmamali
        verify(vanaLogRepository, never()).save(any(VanaLog.class));
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}