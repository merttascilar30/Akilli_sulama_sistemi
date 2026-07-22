package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * /topic/vana-loglari kanaligina yayinlanan, harita uzerinde dinamik
 * konumlandirma yapilabilmesi icin istasyonun enlem/boylam bilgilerini
 * de iceren WebSocket payload'u.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VanaLogYayinDto {

    private UUID istasyonId;
    private Integer durum;
    private String tetiklemeTipi;
    private OffsetDateTime tarih;
    private Double enlem;
    private Double boylam;
}
