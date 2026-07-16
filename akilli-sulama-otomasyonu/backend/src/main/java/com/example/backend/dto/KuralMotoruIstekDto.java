package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Python Kural Motoru servisinin /api/rules/evaluate ucuna gonderilen istek govdesi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KuralMotoruIstekDto {

    private String istasyonId;
    private BigDecimal anlikNem;
    private BigDecimal tarlaKapasitesi;
    private BigDecimal solmaNoktasi;
    private BigDecimal et0;
    private BigDecimal kc;
}
