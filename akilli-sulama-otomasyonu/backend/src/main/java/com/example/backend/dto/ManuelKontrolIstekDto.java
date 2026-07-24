package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * /app/manual-control kanali uzerinden istemciden gelen manuel kontrol istegi.
 * "komut" alani MANUEL, OTONOM veya KAPAT degerlerinden birini alir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManuelKontrolIstekDto {

    private String istasyonId;
    private String komut;
}
