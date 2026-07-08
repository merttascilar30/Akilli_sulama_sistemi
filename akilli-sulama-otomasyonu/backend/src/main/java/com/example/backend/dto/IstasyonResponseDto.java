package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Istasyon bilgilerinin disariya (API cevabi olarak) tasindigi DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IstasyonResponseDto {

    private UUID id;
    private String ad;
    private String koordinat;
    private BigDecimal tarlaKapasitesi;
    private BigDecimal solmaNoktasi;
    private BigDecimal bitkiKatsayisi;
}
