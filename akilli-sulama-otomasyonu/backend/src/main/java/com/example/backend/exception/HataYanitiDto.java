package com.example.backend.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * GlobalExceptionHandler tarafindan istemciye donen standart hata govdesi.
 */
@Getter
@Builder
public class HataYanitiDto {

    private OffsetDateTime zaman;
    private int durumKodu;
    private String hataTipi;
    private String mesaj;
    private String yol;
}
