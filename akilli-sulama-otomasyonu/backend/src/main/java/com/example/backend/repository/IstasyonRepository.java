package com.example.backend.repository;

import com.example.backend.entity.Istasyon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Istasyon entity'si icin veritabani erisim katmani.
 */
public interface IstasyonRepository extends JpaRepository<Istasyon, UUID> {
}
