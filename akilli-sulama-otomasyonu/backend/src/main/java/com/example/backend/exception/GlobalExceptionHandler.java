package com.example.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

/**
 * REST uc noktalarinda olusan hatalari merkezi bir yerden yakalayip istemciye
 * anlamli HTTP durum kodlari ve tutarli bir govde ile donen kuresel hata
 * yakalama katmani. Tum hatalar SLF4J uzerinden loglanir; loglama hedefleri
 * (konsol/dosya) logback-spring.xml icinde tanimlidir.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Istenen kaynak (orn. istasyon) veri tabaninda bulunamadiginda.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<HataYanitiDto> kayitBulunamadiHatasiniIsle(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Kayit bulunamadi [{}]: {}", request.getRequestURI(), ex.getMessage());
        return hataYaniti(HttpStatus.NOT_FOUND, "KAYIT_BULUNAMADI", ex.getMessage(), request);
    }

    /**
     * @Valid ile isaretlenmis istek govdelerinde alan bazli dogrulama hatasi olustugunda.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<HataYanitiDto> dogrulamaHatasiniIsle(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mesaj = ex.getBindingResult().getFieldErrors().stream()
                .map(hata -> hata.getField() + ": " + hata.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Istek govdesi dogrulama hatasi [{}]: {}", request.getRequestURI(), mesaj);
        return hataYaniti(HttpStatus.BAD_REQUEST, "GECERSIZ_VERI", mesaj, request);
    }

    /**
     * Istek govdesi hic parse edilemeyen (bozuk JSON, eksik alan tipi vb.) gecersiz veri formatinda oldugunda.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<HataYanitiDto> okunamayanIstekGovdesiHatasiniIsle(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Istek govdesi okunamadi/gecersiz format [{}]: {}", request.getRequestURI(), ex.getMessage());
        return hataYaniti(HttpStatus.BAD_REQUEST, "GECERSIZ_VERI_FORMATI",
                "Istek govdesi okunamadi veya beklenen formatta degil.", request);
    }

    /**
     * Path/query parametreleri beklenen tipe (orn. UUID) donusturulemediginde.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<HataYanitiDto> parametreTipiUyusmazligiHatasiniIsle(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String mesaj = String.format("'%s' parametresi icin gecersiz deger: %s", ex.getName(), ex.getValue());
        log.warn("Parametre tipi uyusmazligi [{}]: {}", request.getRequestURI(), mesaj);
        return hataYaniti(HttpStatus.BAD_REQUEST, "GECERSIZ_PARAMETRE", mesaj, request);
    }

    /**
     * Servis katmaninda manuel olarak firlatilan gecersiz istek argumanlari icin (orn. UUID.fromString hatasi).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<HataYanitiDto> gecersizArgumanHatasiniIsle(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Gecersiz istek argumani [{}]: {}", request.getRequestURI(), ex.getMessage());
        return hataYaniti(HttpStatus.BAD_REQUEST, "GECERSIZ_ISTEK", ex.getMessage(), request);
    }

    /**
     * PostgreSQL baglanti kopmalari, islem (transaction) hatalari ve diger tum veri tabani erisim sorunlari icin.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<HataYanitiDto> veriTabaniHatasiniIsle(DataAccessException ex, HttpServletRequest request) {
        log.error("Veri tabani erisim hatasi [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return hataYaniti(HttpStatus.SERVICE_UNAVAILABLE, "VERITABANI_ERISIM_HATASI",
                "Veri tabanina su anda erisilemiyor. Lutfen daha sonra tekrar deneyin.", request);
    }

    /**
     * Kural Motoru veya Simulator servisi HTTP uzerinden bir hata durum kodu (4xx/5xx) dondugunde.
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<HataYanitiDto> disServisHttpHatasiniIsle(WebClientResponseException ex, HttpServletRequest request) {
        log.error("Dis servisten hata yaniti alindi [{}]: durum={}, govde={}",
                request.getRequestURI(), ex.getStatusCode(), ex.getResponseBodyAsString());
        return hataYaniti(HttpStatus.BAD_GATEWAY, "DIS_SERVIS_HATASI",
                "Kural motoru veya simulator servisinden gecersiz bir yanit alindi.", request);
    }

    /**
     * Kural Motoru veya Simulator servisine baglanti kurulamadiginda (soket zaman asimi, baglanti reddi vb.).
     */
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<HataYanitiDto> disServisBaglantiHatasiniIsle(WebClientRequestException ex, HttpServletRequest request) {
        log.error("Dis servise baglanti kurulamadi (soket zaman asimi/baglanti hatasi) [{}]: {}",
                request.getRequestURI(), ex.getMessage(), ex);
        return hataYaniti(HttpStatus.GATEWAY_TIMEOUT, "DIS_SERVIS_ZAMAN_ASIMI",
                "Kural motoru veya simulator servisine baglanti kurulamadi.", request);
    }

    /**
     * Yukarida ele alinmayan tum beklenmeyen hatalar icin son cikar (catch-all).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<HataYanitiDto> beklenmeyenHatayiIsle(Exception ex, HttpServletRequest request) {
        log.error("Beklenmeyen bir sunucu hatasi olustu [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return hataYaniti(HttpStatus.INTERNAL_SERVER_ERROR, "BEKLENMEYEN_HATA",
                "Beklenmeyen bir sunucu hatasi olustu.", request);
    }

    private ResponseEntity<HataYanitiDto> hataYaniti(HttpStatus durum, String hataTipi, String mesaj, HttpServletRequest request) {
        HataYanitiDto govde = HataYanitiDto.builder()
                .zaman(OffsetDateTime.now())
                .durumKodu(durum.value())
                .hataTipi(hataTipi)
                .mesaj(mesaj)
                .yol(request.getRequestURI())
                .build();
        return ResponseEntity.status(durum).body(govde);
    }
}
