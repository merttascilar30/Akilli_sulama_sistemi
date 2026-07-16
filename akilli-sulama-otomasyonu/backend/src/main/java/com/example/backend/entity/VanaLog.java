package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Vana ac/kapa hareketlerinin kaydini tutan JPA entity sinifi.
 * "vana_loglari" tablosuyla eslesir.
 */
@Entity
@Table(name = "vana_loglari")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VanaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "istasyon_id", nullable = false)
    private UUID istasyonId;

    /**
     * Vana durumu: 1 = Acik, 0 = Kapali.
     */
    @Column(name = "durum")
    private Integer durum;

    /**
     * Tetikleme tipi: OTONOM (kural motoru karariyla) veya MANUEL (kullanici tarafindan).
     */
    @Column(name = "tetikleme_tipi", length = 50)
    private String tetiklemeTipi;

    @Column(name = "tarih")
    private OffsetDateTime tarih;
}
