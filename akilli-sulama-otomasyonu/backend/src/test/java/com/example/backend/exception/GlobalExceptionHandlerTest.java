package com.example.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    public void testKayitBulunamadiHatasi_404Donmeli() {
        when(request.getRequestURI()).thenReturn("/api/istasyonlar/123");

        ResponseEntity<HataYanitiDto> yanit = handler.kayitBulunamadiHatasiniIsle(
                new EntityNotFoundException("Istasyon bulunamadi"), request);

        assertEquals(HttpStatus.NOT_FOUND, yanit.getStatusCode());
        assertEquals("KAYIT_BULUNAMADI", yanit.getBody().getHataTipi());
        assertEquals("/api/istasyonlar/123", yanit.getBody().getYol());
        assertNotNull(yanit.getBody().getZaman());
    }

    @Test
    public void testGecersizArgumanHatasi_400Donmeli() {
        when(request.getRequestURI()).thenReturn("/app/manual-control");

        ResponseEntity<HataYanitiDto> yanit = handler.gecersizArgumanHatasiniIsle(
                new IllegalArgumentException("Gecersiz UUID formati"), request);

        assertEquals(HttpStatus.BAD_REQUEST, yanit.getStatusCode());
        assertEquals("GECERSIZ_ISTEK", yanit.getBody().getHataTipi());
    }

    @Test
    public void testVeriTabaniHatasi_503Donmeli() {
        when(request.getRequestURI()).thenReturn("/api/istasyonlar");

        ResponseEntity<HataYanitiDto> yanit = handler.veriTabaniHatasiniIsle(
                new DataAccessResourceFailureException("Baglanti koptu"), request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, yanit.getStatusCode());
        assertEquals("VERITABANI_ERISIM_HATASI", yanit.getBody().getHataTipi());
    }

    @Test
    public void testDisServisHttpHatasi_502Donmeli() {
        when(request.getRequestURI()).thenReturn("/api/metrics/async");

        WebClientResponseException ex = WebClientResponseException.create(
                500, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], null);

        ResponseEntity<HataYanitiDto> yanit = handler.disServisHttpHatasiniIsle(ex, request);

        assertEquals(HttpStatus.BAD_GATEWAY, yanit.getStatusCode());
        assertEquals("DIS_SERVIS_HATASI", yanit.getBody().getHataTipi());
    }

    @Test
    public void testDisServisBaglantiHatasi_504Donmeli() {
        when(request.getRequestURI()).thenReturn("/api/metrics/async");

        WebClientRequestException ex = new WebClientRequestException(
                new RuntimeException("Baglanti zaman asimina ugradi"),
                HttpMethod.POST, URI.create("http://localhost:8000/api/rules/evaluate"), new HttpHeaders());

        ResponseEntity<HataYanitiDto> yanit = handler.disServisBaglantiHatasiniIsle(ex, request);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, yanit.getStatusCode());
        assertEquals("DIS_SERVIS_ZAMAN_ASIMI", yanit.getBody().getHataTipi());
    }

    @Test
    public void testBeklenmeyenHata_500Donmeli() {
        when(request.getRequestURI()).thenReturn("/api/istasyonlar");

        ResponseEntity<HataYanitiDto> yanit = handler.beklenmeyenHatayiIsle(
                new RuntimeException("Ongorulmeyen bir durum"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, yanit.getStatusCode());
        assertEquals("BEKLENMEYEN_HATA", yanit.getBody().getHataTipi());
    }
}
