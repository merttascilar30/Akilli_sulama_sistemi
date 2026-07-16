package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Python Kural Motoru servisinin /api/rules/evaluate ucundan dondurdugu yanit govdesi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KuralMotoruYanitDto {

    private String istasyonId;
    private BigDecimal anlikNem;
    private BigDecimal esikNemi;
    private BigDecimal etc;
    private boolean sulamaGerekli;
}
