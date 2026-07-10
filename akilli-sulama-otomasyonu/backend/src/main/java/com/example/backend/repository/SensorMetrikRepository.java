package com.example.backend.repository;

import com.example.backend.entity.SensorMetrik;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * SensorMetrik entity'si icin veritabani erisim katmani.
 */
public interface SensorMetrikRepository extends JpaRepository<SensorMetrik, Long> {
}
