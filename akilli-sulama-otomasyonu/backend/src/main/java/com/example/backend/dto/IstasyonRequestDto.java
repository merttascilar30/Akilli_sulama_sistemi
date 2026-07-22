package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Istasyon olusturma / guncelleme isteklerinde kullanilan DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IstasyonRequestDto {

    @NotBlank(message = "Istasyon adi bos olamaz")
    private String ad;

    private Double enlem;

    private Double boylam;

    @NotNull(message = "Tarla kapasitesi zorunludur")
    private BigDecimal tarlaKapasitesi;

    @NotNull(message = "Solma noktasi zorunludur")
    private BigDecimal solmaNoktasi;

    @NotNull(message = "Bitki katsayisi zorunludur")
    private BigDecimal bitkiKatsayisi;
}
