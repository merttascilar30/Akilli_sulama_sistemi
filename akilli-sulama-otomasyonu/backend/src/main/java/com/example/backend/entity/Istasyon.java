package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sulama istasyonlarini temsil eden JPA entity sinifi.
 * "istasyonlar" tablosuyla eslesir.
 */
@Entity
@Table(name = "istasyonlar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Istasyon {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ad", nullable = false, length = 100)
    private String ad;

    @Column(name = "koordinat", length = 100)
    private String koordinat;

    /**
     * Tarla kapasitesi (%): Topragin tutabilecegi maksimum nem orani.
     */
    @Column(name = "tarla_kapasitesi", precision = 5, scale = 2)
    private BigDecimal tarlaKapasitesi;

    /**
     * Solma noktasi (%): Bitkinin su alamaz hale geldigi nem esigi.
     */
    @Column(name = "solma_noktasi", precision = 5, scale = 2)
    private BigDecimal solmaNoktasi;

    /**
     * Bitki katsayisi (Kc): Referans bitki su tuketimini olceklemek icin kullanilir.
     */
    @Column(name = "bitki_katsayisi", precision = 3, scale = 2)
    private BigDecimal bitkiKatsayisi;
}
