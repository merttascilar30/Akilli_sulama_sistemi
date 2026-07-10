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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Istasyonlardan gelen anlik nem/sicaklik olcumlerini temsil eden JPA entity sinifi.
 * "sensor_metrikleri" tablosuyla eslesir.
 */
@Entity
@Table(name = "sensor_metrikleri")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorMetrik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "istasyon_id", nullable = false)
    private UUID istasyonId;

    @Column(name = "nem", precision = 5, scale = 2)
    private BigDecimal nem;

    @Column(name = "sicaklik", precision = 5, scale = 2)
    private BigDecimal sicaklik;

    @Column(name = "kayit_tarihi")
    private OffsetDateTime kayitTarihi;
}
