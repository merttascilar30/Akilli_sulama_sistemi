package com.example.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulatorden gelen anlik sensor olcumlerini tasiyan DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorMetrikRequestDto {

    @NotNull(message = "Istasyon id zorunludur")
    private UUID istasyonId;

    @NotNull(message = "Nem degeri zorunludur")
    private BigDecimal nem;

    @NotNull(message = "Sicaklik degeri zorunludur")
    private BigDecimal sicaklik;
}
